/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Sofia Lemons.
 */

package android.tether;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.tether.dtn.DtnMessage;
import android.tether.system.AndroidTetherCommon;
import android.tether.system.Configuration;
import android.tether.system.CoreTask;
import android.util.Log;

public class TetherApplication extends Application {

	public static final String MSG_TAG = "TETHER -> TetherApplication";
	
	public final String DEFAULT_PASSPHRASE = "abcdefghijklm";
	public final String DEFAULT_LANNETWORK = "192.168.2.0/24";
	public final String DEFAULT_IPADDRESS = "192.168.2.254"; 
	public final String DEFAULT_ENCSETUP   = "wpa_supplicant";
	
	// Devices-Information
	public String deviceType = Configuration.DEVICE_GENERIC; 
	public String interfaceDriver = Configuration.DRIVER_WEXT; 
	
	// WifiManager
	private WifiManager wifiManager;
	//public String tetherNetworkDevice = null;
	
	// Preferences
	public SharedPreferences settings = null;
	public SharedPreferences.Editor preferenceEditor = null;
	
	// Original States
	private static boolean origWifiState = false;
	
	// Supplicant
	public CoreTask.WpaSupplicant wpasupplicant = null;
	// tether.conf
	public CoreTask.TetherConfig tethercfg = null;
	// CoreTask
	public CoreTask coretask = null;
	
	public ArrayList<DtnMessage> dtnMsgs = null;
	
	@Override
	public void onCreate() {
		Log.d(MSG_TAG, "Calling onCreate()");
		
		//create CoreTask
		this.coretask = new CoreTask();
		this.coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
		Log.d(MSG_TAG, "Current directory is "+this.getApplicationContext().getFilesDir().getParent());

        // Check Homedir, or create it
        this.checkDirs(); 
        
        // Set device-information
        this.deviceType = Configuration.getDeviceType();
        this.interfaceDriver = Configuration.getWifiInterfaceDriver(this.deviceType);
        
        // Preferences
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // preferenceEditor
        this.preferenceEditor = settings.edit();
		
        // init wifiManager
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        
        // Supplicant config
        this.wpasupplicant = this.coretask.new WpaSupplicant();
        
        // tether.cfg
        this.tethercfg = this.coretask.new TetherConfig();
        this.tethercfg.read();
        
        this.dtnMsgs = new ArrayList<DtnMessage>();

	}

	@Override
	public void onTerminate() {
		Log.d(MSG_TAG, "Calling onTerminate()");
    	// Stopping Tether
		this.stopTether();
		// Remove all notifications
	}
	
	public void updateConfiguration() {
		
		long startStamp = System.currentTimeMillis();
		
		String ssid = this.settings.getString("ssidpref", "AndroidTether");
        String txpower = this.settings.getString("txpowerpref", "disabled");
        String lannetwork = this.settings.getString("lannetworkpref", DEFAULT_LANNETWORK);
        String ipAddress = this.settings.getString("ipAddress", DEFAULT_IPADDRESS);
        String channel = this.settings.getString("channelpref", "1");
        Log.d(MSG_TAG,"set IP address:"+ipAddress);
        
		// tether.conf
        this.tethercfg.read();
		this.tethercfg.put("device.type", deviceType);
        this.tethercfg.put("tether.mode", "wifi");
        this.tethercfg.put("wifi.essid", ssid);
        this.tethercfg.put("wifi.channel", channel);
		this.tethercfg.put("ip.network", lannetwork.split("/")[0]);
		this.tethercfg.put("ip.gateway", ipAddress);    
		
		if (Configuration.enableFixPersist()) {
			this.tethercfg.put("tether.fix.persist", "true");
		}
		else {
			this.tethercfg.put("tether.fix.persist", "false");
		}
		if (Configuration.enableFixRoute()) {
			this.tethercfg.put("tether.fix.route", "true");
		}
		else {
			this.tethercfg.put("tether.fix.route", "false");
		}
		
		this.tethercfg.put("wifi.interface", this.coretask.getProp("wifi.interface"));
		this.tethercfg.put("wifi.txpower", txpower);
		this.tethercfg.put("wifi.encryption", "open");
		this.tethercfg.put("wifi.encryption.key", "none");
		
		// Make sure to remove wpa_supplicant.conf
		if (this.wpasupplicant.exists()) {
			this.wpasupplicant.remove();
		}			
		
		// determine driver wpa_supplicant
		this.tethercfg.put("wifi.driver", Configuration.getWifiInterfaceDriver(deviceType));
		
		// writing config-file
		if (this.tethercfg.write() == false) {
			Log.e(MSG_TAG, "Unable to update tether.conf!");
		}
		
		Log.d(MSG_TAG, "Creation of configuration-files took ==> "+(System.currentTimeMillis()-startStamp)+" milliseconds.");
	}

	public String exec_Ifconfig(){
		// get the interface	
		String net_interface = this.tethercfg.get("wifi.interface"); 
		String out = this.coretask.runCommand(this.coretask.DATA_FILE_PATH+"/bin/ifconfig "+net_interface);
		return out;
	}
	
	public String getIpAddress_fromIfconfig(){
		String output = exec_Ifconfig();
		return AndroidTetherCommon.extractMatchString("ip (.*) mask", output);
	}
	
	public String exec_Iwconfig(){
		// Return the value of active interface , the other value of not active output as error-stream 
		String out = this.coretask.runRootCommand_getOutput(this.coretask.DATA_FILE_PATH+"/bin/iwconfig");
		return out;
	}
	
	public String getWirelessStatus_fromIwconfig(){
		String output = exec_Iwconfig();
		String result = "";
		String ssid = AndroidTetherCommon.extractMatchString("ESSID:\"(.*)\" ", output);
		String mode = AndroidTetherCommon.extractMatchString("Mode:(.*?) F", output);
		String cell = AndroidTetherCommon.extractMatchString("Cell: (.*)", output);
		String accessPoint = AndroidTetherCommon.extractMatchString("Access Point: (.*)", output);
		result += "ssid:"+ssid+"("+mode+")\n";//cell:"+cell;
		if(cell != null){
			result += "cell:"+cell;
		}
		if(accessPoint != null){
			result += "AccessPoint:"+accessPoint;
		}
		result = (ssid!=null & mode!=null)? result:null;
		return result;
	}
	
	public String getMacAddress(){
		if(this.settings.contains("MacAddress")){
			return this.settings.getString("MacAddress",null);
		}
		else{
			String macAddress = getMacAddress_fromBusyBox_ifconfig();
			this.preferenceEditor.putString("MacAddress", macAddress);
			this.preferenceEditor.commit();
			return macAddress;
		}
	}
	public String getMacAddress_fromBusyBox_ifconfig(){
		String net_interface = this.tethercfg.get("wifi.interface"); 
		String output =  this.coretask.runCommand("busybox ifconfig " + net_interface);
		String macAddress = AndroidTetherCommon.extractMatchString("HWaddr (.*)", output);
		return macAddress;
	}
	
	public String excec_Iwlist(){
		String out = this.coretask.runRootCommand_getOutput(this.coretask.DATA_FILE_PATH+"/bin/iwlist scan");
		return out;
	}
	
	public boolean settingIp(String ipAddress){
		String net_interface = this.tethercfg.get("wifi.interface"); 
		this.preferenceEditor.putString("ipAddress",ipAddress);
		this.preferenceEditor.commit();
		boolean result = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/ifconfig "+net_interface+" "+ipAddress+" netmask 255.255.255.0");
		return result;
	}
	public String getIpAddress(){
		return this.settings.getString("ipAddress", DEFAULT_IPADDRESS);
	}

	
	// Start/Stop Tethering
    public boolean startTether() {

        // Updating all configs
        this.updateConfiguration();

        this.disableWifi();

    	// Starting service
    	if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether start 1")) {
    		return true;
    	}
    	return false;
    }
    
    public boolean stopTether() {
    	boolean stopped = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether stop 1");
		this.enableWifi();
		return stopped;
    }
	
    public boolean restartTether() {
    	boolean status = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether stop 1");
    	
        // Updating all configs
        this.updateConfiguration();       
	    this.disableWifi();
    	// Starting service
        if (status == true)
        	status = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/tether start 1");
        
    	return status;
    }
    
    public String getTetherNetworkDevice() {
		return this.coretask.getProp("wifi.interface");
    }
    
    // gets user preference on whether sync should be disabled during tethering
    public boolean isUpdatecDisabled(){
		return this.settings.getBoolean("updatepref", false);
	}
    
    // Wifi
    public void disableWifi() {
    	if (this.wifiManager.isWifiEnabled()) {
    		origWifiState = true;
    		this.wifiManager.setWifiEnabled(false);
    		Log.d(MSG_TAG, "Wifi disabled!");
        	// Waiting for interface-shutdown
    		try {
    			Thread.sleep(5000);
    		} catch (InterruptedException e) {
    			// nothing
    		}
    	}
    }
    
    public void enableWifi() {
    	if (origWifiState) {
        	// Waiting for interface-restart
    		this.wifiManager.setWifiEnabled(true);
    		try {
    			Thread.sleep(5000);
    		} catch (InterruptedException e) {
    			// nothing
    		}
    		Log.d(MSG_TAG, "Wifi started!");
    	}
    }
    
    public boolean binariesExists() {
    	File file = new File(this.coretask.DATA_FILE_PATH+"/bin/tether");
    	return file.exists();
    }
    
    public void installWpaSupplicantConfig() {
    	this.copyFile(this.coretask.DATA_FILE_PATH+"/conf/wpa_supplicant.conf", "0644", R.raw.wpa_supplicant_conf);
    }
    
    public void installFiles() {
    	new Thread(new Runnable(){
			public void run(){
				String message = null;
				// tether
		    	if (message == null) {
			    	message = TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/tether", "0755", R.raw.tether);
		    	}
		    	// ifconfig
		    	if (message == null) {
			    	message = TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/ifconfig", "0755", R.raw.ifconfig);
		    	}	
		    	// iwconfig
		    	if (message == null) {
			    	message = TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/iwconfig", "0755", R.raw.iwconfig);
		    	}
		    	// iwlist
		    	if (message == null) {
		    		message = TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/iwlist", "0755", R.raw.iwlist);
		    	}
				
				// edify script
				if (message == null) {
					TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.edify", "0644", R.raw.tether_edify);
				}
				// tether.cfg
				if (message == null) {
					TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.conf", "0644", R.raw.tether_conf);
				}
				
				// wpa_supplicant drops privileges, we need to make files readable.
				TetherApplication.this.coretask.chmod(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/", "0755");

			}
		}).start();
    }
    
    private String copyFile(String filename, String permission, int ressource) {
    	String result = this.copyFile(filename, ressource);
    	if (result != null) {
    		return result;
    	}
    	if (this.coretask.chmod(filename, permission) != true) {
    		result = "Can't change file-permission for '"+filename+"'!";
    	}
    	return result;
    }
    
    private String copyFile(String filename, int ressource) {
    	File outFile = new File(filename);
    	Log.d(MSG_TAG, "Copying file '"+filename+"' ...");
    	InputStream is = this.getResources().openRawResource(ressource);
    	byte buf[] = new byte[1024];
        int len;
        try {
        	OutputStream out = new FileOutputStream(outFile);
        	while((len = is.read(buf))>0) {
				out.write(buf,0,len);
			}
        	out.close();
        	is.close();
		} catch (IOException e) {
			return "Couldn't install file - "+filename+"!";
		}
		return null;
    }
    
    private void checkDirs() {
    	File dir = new File(this.coretask.DATA_FILE_PATH);
    	if (dir.exists() == false) {
    			Log.d("test","Application data-dir does not exist!");
    	}
    	else {
    		//String[] dirs = { "/bin", "/var", "/conf", "/library" };
    		String[] dirs = { "/bin", "/var", "/conf" };
    		for (String dirname : dirs) {
    			dir = new File(this.coretask.DATA_FILE_PATH + dirname);
    	    	if (dir.exists() == false) {
    	    		if (!dir.mkdir()) {
    	    			Log.d("test","Couldn't create " + dirname + " directory!");
    	    		}
    	    	}
    	    	else {
    	    		Log.d(MSG_TAG, "Directory '"+dir.getAbsolutePath()+"' already exists!");
    	    	}
    		}
    	}
    }
    
    public int getVersionNumber() {
    	int version = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionCode;
        } catch (Exception e) {
            Log.e(MSG_TAG, "Package name not found", e);
        }
        return version;
    }
    
    public String getVersionName() {
    	String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (Exception e) {
            Log.e(MSG_TAG, "Package name not found", e);
        }
        return version;
    }

}
