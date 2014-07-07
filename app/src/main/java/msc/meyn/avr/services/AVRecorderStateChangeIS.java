package msc.meyn.avr.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.activities.VideoListBrowser;
import msc.meyn.avr.support.Utils;

public class AVRecorderStateChangeIS extends IntentService {

	private final static String CLASS_NAME = AVRecorderStateChangeIS.class
			.getName();

	private AtomicBoolean mInitializationInProgress;
	private AVRecorderApp mAppReference;

	public static void performAction(Context context, String action) {
		performAction(context, action, null);
	}

	public static void performAction(Context context, String action,
			Bundle extras) {
		if ((context == null) || (action == null) || action.trim().isEmpty()) {
			return;
		}
		Intent svc = new Intent(context, AVRecorderStateChangeIS.class);
		svc.setAction(action);
		if (extras != null) {
			svc.putExtras(extras);
		}
		context.startService(svc);
	}

	public AVRecorderStateChangeIS() {
		super("AppInitIntentService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mAppReference = (AVRecorderApp) getApplicationContext();
		mInitializationInProgress = new AtomicBoolean(false);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(CLASS_NAME, "onHandleIntent(): " + intent);
		String action = intent.getAction();
		if ((action != null) && !(action.trim().isEmpty())) {
			if (action.equals(Constants.ACTION_APP_STATE_CHANGE)) {
				synchronized (this) {
					if (mAppReference.isMediaReady()
							&& mAppReference.isNetworkAvailable()) {
						startApp();
					} else {
						stopApp();
					}
				}
			}
		}
	}

	/**
	 * 
	 */
	private void startApp() {
		Log.d(CLASS_NAME, "startApp()");
		if (mInitializationInProgress.compareAndSet(false, true)) {
			Context context = getApplicationContext();
			setupLogsDir();
			copyTcpdumpExe();
			if (mAppReference.isDownloadRequired()) {
				startActivity(new Intent(context, VideoListBrowser.class)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			} else {
				startService(new Intent(context, HandleSchedulingIS.class));
			}
			mInitializationInProgress.set(false);
		}
	}

	protected void stopApp() {
		Log.d(CLASS_NAME, "stopApp(): " + mAppReference.isMediaReady() + ", "
				+ mAppReference.isNetworkAvailable());
		PlayVideosIS.cancelScheduledPlay(getApplicationContext());
	}

	/**
	 * Checks for the presence of the Log directory on the sdcard and creates it
	 * if needed
	 */
	private void setupLogsDir() {
		String packageName = this.getPackageName();
		File logDir = Environment.getExternalStoragePublicDirectory(packageName
				+ "/" + Constants.LOCAL_LOG_DIR_NAME);
		if (!logDir.isDirectory()) {
			if (!logDir.mkdirs()) {
				Log.e(CLASS_NAME, "Failed creating directory: " + logDir);
			}
		}
	}

	/**
	 * Copies the tcpdump executable as a raw resource from the assets directory
	 * to the app storage directory on disk and makes it executable.
	 */
	private void copyTcpdumpExe() {
		AssetManager assetManager = getAssets();
		String tcpdumpFileName = Constants.TCPDUMP_EXE_NAME;
		File executable = getFileStreamPath(tcpdumpFileName);
		if (!executable.canExecute()) {
			try {
				Utils.copyFile(assetManager.open(Constants.TCPDUMP_ASSET_NAME),
						openFileOutput(tcpdumpFileName, Context.MODE_PRIVATE));
				executable.setExecutable(true);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(CLASS_NAME, "createTcpdumpExe(): " + e.getMessage());
			}
		}
	}

}
