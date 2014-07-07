package msc.meyn.avr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import msc.meyn.avr.AVRecorderApp;

public class MediaMountedReceiver extends BroadcastReceiver {

	private final static String LOGTAG = MediaMountedReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(LOGTAG, "onReceive(): " + intent.getAction());
		if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
			Log.d(LOGTAG, "MEDIA_MOUNTED received");
			((AVRecorderApp) context.getApplicationContext())
					.setMediaAvailability(true);
		} else {
			((AVRecorderApp) context.getApplicationContext())
					.setMediaAvailability(false);
		}
	}

}
