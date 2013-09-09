package android.tether.dtn.sensor;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.app.Activity;
import android.content.Context;

public class DtnMagnetismSensorEvent implements SensorEventListener {
	public static final String MSG_TAG = "DTN -> Sensor -> Magnetism";
	public FetchShakeAlgorithm fetchShakeJudgeAlgorithm;
	public FileOutputStream fileOutputStream = null;
	
	public DtnMagnetismSensorEvent(Context ctx){ 
		try {
			Log.d(MSG_TAG,"Open file");
			this.fileOutputStream = ctx.openFileOutput("magnetic.csv", Activity.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			Log.d(MSG_TAG,"Can't open file");
			e.printStackTrace();
		}
	}
	
	public void stop(){
		try {
			Log.d(MSG_TAG,"Close file");
			this.fileOutputStream.close();
		} catch (IOException e) {
			Log.d(MSG_TAG,"Can't close file");
			e.printStackTrace();
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		String str = "";
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			str = x+","+y+","+z+"\n";
			if(this.fileOutputStream != null){
				try {
					this.fileOutputStream.write(str.getBytes());
				} catch (IOException e) {
					Log.d(MSG_TAG,"Can't write csv file");
					e.printStackTrace();
				}
			}
		}
	}
}
