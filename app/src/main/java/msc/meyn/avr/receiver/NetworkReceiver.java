package msc.meyn.avr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import msc.meyn.avr.AVRecorderApp;

public class NetworkReceiver extends BroadcastReceiver {

	private final static String CLASS_NAME = NetworkReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(CLASS_NAME, "onReceive(): " + context + ", " + intent);
		NetworkInfo activeNetwork = ((ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnected()) {
			((AVRecorderApp) context.getApplicationContext())
					.setNetworkAvailablity(true);
		} else {
			((AVRecorderApp) context.getApplicationContext())
					.setNetworkAvailablity(false);
		}
	}

}
