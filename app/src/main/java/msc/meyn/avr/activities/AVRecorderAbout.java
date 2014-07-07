package msc.meyn.avr.activities;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

import msc.meyn.avr.R;

public class AVRecorderAbout extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_activity_about);

		final TextView versionInfo = (TextView) findViewById(R.id.about_version);
		String version;
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pInfo.versionName + " (" + pInfo.versionCode + ")";
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			version = "unknown";
		}
		versionInfo.setText("Version: " + version);
	}

}
