package android.tether.dtn.sensor;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.tether.TetherApplication;
import android.content.Context;

public class DtnMagnetismSensorEvent implements SensorEventListener {
	public static final String MSG_TAG = "DTN -> Sensor -> Magnetism";
	
	public TetherApplication app;
	
	public DtnMagnetismSensorEvent(TetherApplication app){
		this.app = app;
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			this.app.addRawManetic(x, y);
		}
	}
}
