package msc.meyn.avr;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import msc.meyn.avr.services.AVRecorderStateChangeIS;
import msc.meyn.avr.services.HandleSchedulingIS;
import msc.meyn.avr.support.VideoList;

public class AVRecorderApp extends Application {

	private final static String CLASS_NAME = AVRecorderApp.class.getName();

	private static VideoList mVideoList = null;
	private static AtomicBoolean mTrainingInProgress = new AtomicBoolean(false);
	private static AtomicBoolean mCaptureInProgress = new AtomicBoolean(false);
	private static boolean mIsMediaReady = false;
	private static boolean mIsNetworkAvailable = false;

	@Override
	public void onCreate() {
		Log.v(CLASS_NAME, "onCreate()");
		AVRecorderStateChangeIS.performAction(this,
				Constants.ACTION_APP_STATE_CHANGE);
	}

	/**
	 * A download is required if, <li>
	 * There is no stored list of videos or the current played count doesn't
	 * exist</li> <li>
	 * A stored list has been played the minimum number of times</li> <li>
	 * There is an error interpreting the stored list</li> <li>
	 * A stored list has expired</li>
	 * 
	 * @return <code>true</code> if a download is required at this time
	 *         <code>false</code> otherwise
	 */
	public boolean isDownloadRequired() {
		Log.v(CLASS_NAME, "isDownloadRequired()");
		mVideoList = getCurrentList();
		if (mVideoList != null) {
			if (!mVideoList.hasExpired()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param status
	 */
	public void setMediaAvailability(boolean status) {
		Log.v(CLASS_NAME, "setMediaReady(): " + status);
		if (status != mIsMediaReady) {
			mIsMediaReady = status;
			AVRecorderStateChangeIS.performAction(this,
					Constants.ACTION_APP_STATE_CHANGE);
		}
	}

	/**
	 * 
	 * @param status
	 */
	public void setNetworkAvailablity(boolean status) {
		Log.v(CLASS_NAME, "setNetworkAvailable(): " + status);
		if (status != mIsNetworkAvailable) {
			mIsNetworkAvailable = status;
			AVRecorderStateChangeIS.performAction(this,
					Constants.ACTION_APP_STATE_CHANGE);
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean isMediaReady() {
		if (!mIsMediaReady) {
			mIsMediaReady = Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState());
		}
		return mIsMediaReady;
	}

	public boolean isNetworkAvailable() {
		if (!mIsNetworkAvailable) {
			mIsNetworkAvailable = isOnline();
		}
		return mIsNetworkAvailable;
	}

	public boolean isOnline() {
		NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnected();
	}

	/**
	 * 
	 */
	public VideoList removeCurrentList() {
		if (mVideoList != null) {
			mVideoList = mVideoList.removeList(this);
		}
		return mVideoList;
	}

	/**
	 * 
	 * @param list
	 * @param listStrRep
	 * @return
	 */
	public VideoList setCurrentList(VideoList list, String listStrRep) {
		mVideoList = removeCurrentList();
		if (list != null && listStrRep != null) {
			mVideoList = list.storeList(this, listStrRep);
			startService(new Intent(this, HandleSchedulingIS.class));
		}
		return mVideoList;
	}

	public VideoList getCurrentList() {
		Log.v(CLASS_NAME, "getCurrentList(): " + (mVideoList != null));
		if (mVideoList != null) {
			return mVideoList;
		}
		String listObjStr = Storage.readVideoList(PreferenceManager
				.getDefaultSharedPreferences(this));
		if (listObjStr != null) {
			try {
				mVideoList = new VideoList(new JSONObject(listObjStr), this);
			} catch (JSONException e) {
				e.printStackTrace();
				mVideoList = null;
			}
		}
		return mVideoList;
	}

	public boolean setTrainingInProgress() {
		return !mCaptureInProgress.get()
				&& mTrainingInProgress.compareAndSet(false, true);
	}

	public void setTrainingCompleted() {
		mTrainingInProgress.set(false);
	}

	public boolean setCaptureInProgress() {
		return !mTrainingInProgress.get()
				&& mCaptureInProgress.compareAndSet(false, true);
	}

	public void setCaptureCompleted() {
		mCaptureInProgress.set(false);
	}

}
