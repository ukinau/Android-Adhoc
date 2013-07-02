package android.tether.dtn.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class DtnAccelerateSensorEvent implements SensorEventListener{
	public static final String MSG_TAG = "DTN -> Sensor -> Accelerater";
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
			float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            Log.d(MSG_TAG, "x:" + x + ":y:" + y + ":z:" + z);
		}
	}

	public void onAccuracyChanged(Sensor paramSensor, int paramInt) {
	}

	
}
