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


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class SettingActivity extends Activity {
	
	private TetherApplication application = null;

	private ImageButton startBtn = null;
	private OnClickListener startBtnListener = null;
	private ImageButton stopBtn = null;
	private OnClickListener stopBtnListener = null;
	private Button ifconfigBtn = null;
	private Button iwconfigBtn = null;
	private Button iwlistBtn = null;
	private Button settingIpBtn = null;
	private Button getMacAddressBtn = null;
	
	public static final int MESSAGE_CHECK_LOG = 1;
	public static final int MESSAGE_CANT_START_TETHER = 2;
	public static final int MESSAGE_DOWNLOAD_STARTING = 3;
	public static final int MESSAGE_DOWNLOAD_PROGRESS = 4;
	public static final int MESSAGE_DOWNLOAD_COMPLETE = 5;
	public static final int MESSAGE_DOWNLOAD_BLUETOOTH_COMPLETE = 6;
	public static final int MESSAGE_DOWNLOAD_BLUETOOTH_FAILED = 7;
	public static final int MESSAGE_TRAFFIC_START = 8;
	public static final int MESSAGE_TRAFFIC_COUNT = 9;
	public static final int MESSAGE_TRAFFIC_RATE = 10;
	public static final int MESSAGE_TRAFFIC_END = 11;
	
	public static final String MSG_TAG = "TETHER -> MainActivity";
	public static SettingActivity currentInstance = null;
	
    private static void setCurrent(SettingActivity current){
    	SettingActivity.currentInstance = current;
    }
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(MSG_TAG, "Calling onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_content);
        
        // Init Application
        this.application = (TetherApplication)this.getApplication();
        SettingActivity.setCurrent(this);
        
    	// Check if binaries need to be updated
    	if (this.application.binariesExists() == false || this.application.coretask.filesetOutdated()) {
        	if (this.application.coretask.hasRootPermission()) {
        		this.application.installFiles();
        	}
        }
        
        // Start Button
        this.startBtn = (ImageButton)findViewById(R.id.startTetherBtn);
        this.startBtnListener = new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "StartBtn pressed ...");
				new Thread(new Runnable(){
					public void run(){
						boolean started = SettingActivity.this.application.startTether();
					}
				}).start();
			}
		};
		this.startBtn.setOnClickListener(this.startBtnListener);

		// Stop Button
		this.stopBtn = (ImageButton) findViewById(R.id.stopTetherBtn);
		this.stopBtnListener = new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "StopBtn pressed ...");
				new Thread(new Runnable(){
					public void run(){
						SettingActivity.this.application.stopTether();
					}
				}).start();
			}
		};
		this.stopBtn.setOnClickListener(this.stopBtnListener);
		
		this.ifconfigBtn = (Button)findViewById(R.id.ifconfig);
	    this.ifconfigBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View v){
	    		String out = SettingActivity.this.application.exec_Ifconfig();
	    		EditText result = (EditText)findViewById(R.id.result);
	    		result.setText(out);
	    	}
	    	
	    });
	    
	    this.iwconfigBtn = (Button)findViewById(R.id.iwconfig);
	    this.iwconfigBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View v){
	    		String out =  SettingActivity.this.application.exec_Iwconfig();
	    		EditText result = (EditText)findViewById(R.id.result);
	    		result.setText(out);
	    	}
	    });
	    
	    this.iwlistBtn = (Button)findViewById(R.id.iwlist);
	    this.iwlistBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View v){
	    		String out = SettingActivity.this.application.excec_Iwlist();
	    		EditText result = (EditText)findViewById(R.id.result);
	    		result.setText(out);
	    	}
	    });
	    this.getMacAddressBtn = (Button)findViewById(R.id.getMacAddress);
	    this.getMacAddressBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View v){
	    		String out = SettingActivity.this.application.getMacAddress();
	    		EditText result = (EditText)findViewById(R.id.result);
	    		result.setText(out);
	    	}
	  	});
	    
	    
	    EditText ipField = (EditText)findViewById(R.id.ipAddressSetting);
	    ipField.setText(this.application.getIpAddress());
	    
	    this.settingIpBtn = (Button)findViewById(R.id.ipAddressSettingButton);
	    this.settingIpBtn.setOnClickListener(new OnClickListener(){
	    	public void onClick(View v){
	    		EditText ipField = (EditText)findViewById(R.id.ipAddressSetting);
	    		boolean result = SettingActivity.this.application.settingIp(ipField.getText().toString());
	    		if(result)
	    			Toast.makeText(SettingActivity.this, "IPアドレスを["+ipField.getText().toString()+"]に設定", Toast.LENGTH_LONG).show();
	    		else
	    			Toast.makeText(SettingActivity.this, "IPアドレス設定に失敗", Toast.LENGTH_LONG).show();
	    	}
	    });
		
		
		
    }
    
	public void onStop() {
    	Log.d(MSG_TAG, "Calling onStop()");
		super.onStop();
	}

	public void onDestroy() {
    	Log.d(MSG_TAG, "Calling onDestroy()");
    	super.onDestroy();
	}

	public void onResume() {
		Log.d(MSG_TAG, "Calling onResume()");
		super.onResume();
	}
	
}

