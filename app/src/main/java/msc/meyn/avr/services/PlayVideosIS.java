package msc.meyn.avr.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.activities.RecorderController;
import msc.meyn.avr.support.VideoList;

public class PlayVideosIS extends IntentService {

	private final static String CLASS_NAME = PlayVideosIS.class
			.getName();

	public static final String ACTION_AUTO_VIDEO_LIST_PLAY = CLASS_NAME
			+ ".ACTION_AUTO_PLAY";
	public static final String ACTION_MANUAL_VIDEO_LIST_PLAY = CLASS_NAME
			+ ".ACTION_MANUAL_PLAY";
	public static final String ACTION_POSTPONED_VIDEO_LIST_PLAY = CLASS_NAME
			+ ".ACTION_POSTPONED_VIDEO_LIST_PLAY";
	public static final String EXTRA_VERFIY = CLASS_NAME + ".EXTRA_VERIFY";

	public static void cancelScheduledPlay(Context context) {
		Intent playActivityIntent = new Intent(context,
				RecorderController.class).setAction(
				ACTION_AUTO_VIDEO_LIST_PLAY).addFlags(
				Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent.getActivity(context,
				Constants.SCHEDULED_PLAY_PENDING_INTENT_CODE,
				playActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT).cancel();
		Log.v(CLASS_NAME, "cancelScheduledPlay()");
	}

	public PlayVideosIS() {
		super(CLASS_NAME);
	}

	protected void doWakefulWork(Intent intent) {
		Log.v(CLASS_NAME, "doWakefulWork(): " + intent);
		String action = intent.getAction();
		if (action != null) {
			Intent playActivityIntent = new Intent(getApplicationContext(),
					RecorderController.class)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			if (action.equals(ACTION_MANUAL_VIDEO_LIST_PLAY)) {
				playActivityIntent.setAction(action);
				startActivity(playActivityIntent);
			} else {
				VideoList list = ((AVRecorderApp) getApplicationContext())
						.getCurrentList();
				if (list != null) {
					Calendar autoRunTime = list.getNextExecutionTime();
					if (autoRunTime != null) {
						playActivityIntent
								.setAction(ACTION_AUTO_VIDEO_LIST_PLAY);
						if (action.equals(ACTION_POSTPONED_VIDEO_LIST_PLAY)) {
							autoRunTime = Calendar.getInstance();
							autoRunTime.add(Calendar.MINUTE, 2); // TODO: change
																	// TBD
						}
						PendingIntent pi = PendingIntent.getActivity(
								getApplicationContext(),
								Constants.SCHEDULED_PLAY_PENDING_INTENT_CODE,
								playActivityIntent,
								PendingIntent.FLAG_UPDATE_CURRENT);
						AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
						alarmMgr.set(AlarmManager.RTC_WAKEUP,
								autoRunTime.getTimeInMillis(), pi);
						SimpleDateFormat sdf = new SimpleDateFormat(
								"MMM-dd HH:mm:ss");
						Log.d(CLASS_NAME,
								"scheduled" + pi + " : for "
										+ sdf.format(autoRunTime.getTime()));
					}
				}
			}
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(CLASS_NAME, "onHandleIntent(): " + intent);
		String action = intent.getAction();

		if (action != null) {
			Intent playActivityIntent = new Intent(getApplicationContext(),
					RecorderController.class)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			if (action.equals(ACTION_MANUAL_VIDEO_LIST_PLAY)) {
				playActivityIntent.setAction(action);
				startActivity(playActivityIntent);
			} else {
				VideoList list = ((AVRecorderApp) getApplicationContext())
						.getCurrentList();
				if (list != null) {
					Calendar autoRunTime = list.getNextExecutionTime();
					if (autoRunTime != null) {
						playActivityIntent
								.setAction(ACTION_AUTO_VIDEO_LIST_PLAY);
						if (action.equals(ACTION_POSTPONED_VIDEO_LIST_PLAY)) {
							autoRunTime = Calendar.getInstance();
							autoRunTime.add(Calendar.MINUTE, 1); // TODO: change
						}
						PendingIntent pi = PendingIntent.getActivity(
								getApplicationContext(),
								Constants.SCHEDULED_PLAY_PENDING_INTENT_CODE,
								playActivityIntent,
								PendingIntent.FLAG_UPDATE_CURRENT);
						AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
						alarmMgr.set(AlarmManager.RTC_WAKEUP,
								autoRunTime.getTimeInMillis(), pi);
						SimpleDateFormat sdf = new SimpleDateFormat(
								"MMM-dd HH:mm:ss");
						Log.d(CLASS_NAME,
								"scheduled" + pi + " : for "
										+ sdf.format(autoRunTime.getTime()));
					}
				}
			}
		}
	}

}
