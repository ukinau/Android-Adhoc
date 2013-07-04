package android.tether;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.TabHost;
import android.widget.TabWidget;

public class TabRootActivity extends TabActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_host);
		initTabs();
		TabWidget tab_widget = this.getTabWidget();
		int tab_count = tab_widget.getChildCount();
		for (int i = 0; i < tab_count; i++) {
			tab_widget.getChildTabViewAt(i).getLayoutParams().height = LayoutParams.WRAP_CONTENT;
		}
	}

	public void initTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;

		// SettingActivity
		intent = new Intent().setClass(this, SettingActivity.class);
		spec = tabHost
				.newTabSpec("Tab1")
				.setIndicator("Setting", res.getDrawable(R.drawable.setting_tab))
				.setContent(intent);
		tabHost.addTab(spec);

		// CommunicateActivity
		intent = new Intent().setClass(this, CommunicateActivity.class);
		spec = tabHost
				.newTabSpec("Tab2")
				.setIndicator("Chat", res.getDrawable(R.drawable.communicate_tab))
				.setContent(intent);
		tabHost.addTab(spec);

		// DtnActivity
		intent = new Intent().setClass(this, DtnActivity.class);
		spec = tabHost.newTabSpec("Tab3")
				.setIndicator("DTN", res.getDrawable(R.drawable.dtn_tab))
				.setContent(intent);
		tabHost.addTab(spec);

	}

}
