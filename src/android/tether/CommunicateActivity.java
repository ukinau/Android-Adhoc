package android.tether;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class CommunicateActivity extends Activity {
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    TextView view = new TextView(this);
	    view.setText("communicate");
	    setContentView(view);
	  }
}
