package android.tether;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class DtnActivity extends Activity {
	
	public void onCreate(Bundle savedInstanced){
		super.onCreate(savedInstanced);
	    TextView view = new TextView(this);
	    view.setText("communicate");
		setContentView(view);
	}

}
