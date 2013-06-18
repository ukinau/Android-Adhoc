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

public class MainActivity extends Activity {
	
	private TetherApplication application = null;

	private Button startBtn = null;
	private OnClickListener startBtnListener = null;
	private Button stopBtn = null;
	private OnClickListener stopBtnListener = null;
	
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
	public static MainActivity currentInstance = null;
	
    private static void setCurrent(MainActivity current){
    	MainActivity.currentInstance = current;
    }
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(MSG_TAG, "Calling onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Init Application
        this.application = (TetherApplication)this.getApplication();
        MainActivity.setCurrent(this);
        
    	// Check if binaries need to be updated
    	if (this.application.binariesExists() == false || this.application.coretask.filesetOutdated()) {
        	if (this.application.coretask.hasRootPermission()) {
        		this.application.installFiles();
        	}
        }
        
        // Start Button
        this.startBtn = (Button)findViewById(R.id.startTetherBtn);
        this.startBtnListener = new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "StartBtn pressed ...");
				new Thread(new Runnable(){
					public void run(){
						boolean started = MainActivity.this.application.startTether();
					}
				}).start();
			}
		};
		this.startBtn.setOnClickListener(this.startBtnListener);

		// Stop Button
		this.stopBtn = (Button) findViewById(R.id.stopTetherBtn);
		this.stopBtnListener = new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "StopBtn pressed ...");
				new Thread(new Runnable(){
					public void run(){
						MainActivity.this.application.stopTether();
					}
				}).start();
			}
		};
		this.stopBtn.setOnClickListener(this.stopBtnListener);
		
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

