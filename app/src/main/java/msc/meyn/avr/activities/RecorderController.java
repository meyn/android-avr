package msc.meyn.avr.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.util.ArrayList;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.R;
import msc.meyn.avr.services.PlayVideosIS;
import msc.meyn.avr.support.VideoItem;
import msc.meyn.avr.support.VideoList;

public class RecorderController extends Activity implements
		OnClickListener {

	private final static String CLASS_NAME = RecorderController.class
			.getName();

	private final static String KEY_INTENT_INDEX = "KEY_INTENT_INDEX";
	private final static String KEY_INTENT_DATA_LIST = "KEY_INTENT_DATA_LIST";
	private final static String KEY_INTENT_INTRA_LIST_INTERVAL = "KEY_INTRA_LIST_INTERVAL";
	private final static String KEY_INTENT_SUCCESS_COUNT = "KEY_SUCCESS_COUNT";
	private final static String KEY_INTENT_IS_AUTOPLAY = "KEY_IS_AUTOPLAY";

	private final static int VIDEO_CAPTURE_ACTIVITY_REQ_CODE = 1;

	private int mCurrentIntentReqIndex;
	private ArrayList<VideoItem> mVideoList = new ArrayList<VideoItem>();
	private int mIntraListInterval;
	private int mSuccessfulPlaybacks;
	private boolean mIsAutoPlay = false;

	private CountDownTimer mPlaybackStartCountDown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (((AVRecorderApp) getApplication()).setCaptureInProgress() == false) {
			postponeAutoPlay();
			Log.e(CLASS_NAME, "POSTPONING due to conflict");
			return;
		}

		this.setFinishOnTouchOutside(false);
		setContentView(R.layout.dialog_activity_recorder_controller);

		((TextView) findViewById(R.id.captureMgrMsg)).setText(getResources()
				.getString(R.string.capture_beginning_msg));

		String action = getIntent().getAction();
		VideoList list = ((AVRecorderApp) getApplication()).getCurrentList();
		if (list != null) {
			if (getIntent().getBooleanExtra(
					PlayVideosIS.EXTRA_VERFIY, false)) {
				mVideoList = list.getTrainedVideoItemsSample(this);
				mIntraListInterval = 1;
			} else {
				mVideoList = list.getTrainedVideoItems(this);
				mIntraListInterval = list.getSchedule().getIntraListInterval();
			}
			orientActivity();
			if ((action != null)
					&& (action
							.equals(PlayVideosIS.ACTION_AUTO_VIDEO_LIST_PLAY))) {
				if (!((AVRecorderApp) getApplication()).isOnline()) {
					postponeAutoPlay();
				} else {
					mIsAutoPlay = true;
					mPlaybackStartCountDown = startCapturePlayCountdown(20);//TODO:??
					findViewById(R.id.autoPlayPostponeButton)
							.setOnClickListener(this);
					findViewById(R.id.autoPlayStartButton).setOnClickListener(
							this);
				}
			} else {
				mPlaybackStartCountDown = startCapturePlayCountdown(0);
			}
		} else {
			finishThisActivity(RESULT_CANCELED);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.v(CLASS_NAME, "onConfigurationChanged(): " + newConfig);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		Log.v(CLASS_NAME, "onBackPressed()");
		// disallow dismissing via back button until all done
		if (mCurrentIntentReqIndex >= mVideoList.size()) {
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_INTENT_INDEX, mCurrentIntentReqIndex);
		outState.putParcelableArrayList(KEY_INTENT_DATA_LIST, mVideoList);
		outState.putInt(KEY_INTENT_INTRA_LIST_INTERVAL, mIntraListInterval);
		outState.putInt(KEY_INTENT_SUCCESS_COUNT, mSuccessfulPlaybacks);
		outState.putBoolean(KEY_INTENT_IS_AUTOPLAY, mIsAutoPlay);
		Log.d(CLASS_NAME, "onSaveInstanceState(): " + outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mCurrentIntentReqIndex = savedInstanceState.getInt(KEY_INTENT_INDEX);
		mVideoList = savedInstanceState
				.getParcelableArrayList(KEY_INTENT_DATA_LIST);
		mIntraListInterval = savedInstanceState
				.getInt(KEY_INTENT_INTRA_LIST_INTERVAL);
		mSuccessfulPlaybacks = savedInstanceState
				.getInt(KEY_INTENT_SUCCESS_COUNT);
		mIsAutoPlay = savedInstanceState.getBoolean(KEY_INTENT_IS_AUTOPLAY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(CLASS_NAME, "onActivityResult(): " + requestCode + ", "
				+ resultCode + ", " + data);
		switch (requestCode) {
		case VIDEO_CAPTURE_ACTIVITY_REQ_CODE:
			if (resultCode == RESULT_OK) {
				++mSuccessfulPlaybacks;
			}
			++mCurrentIntentReqIndex;
			sendNextIntent();
			break;
		}
	}

	private int getScreenOrientationPref() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String value = sharedPrefs.getString("pref_key_video_oriention_value",
				"2").trim();
		Log.d(CLASS_NAME, "getOrientationPref(): " + value);
		if (value.equals("1")) {
			return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;// Configuration.ORIENTATION_PORTRAIT;
		}
		return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;// Configuration.ORIENTATION_LANDSCAPE;
	}

	private int getConfigOrientationPref() {
		return (getScreenOrientationPref() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE);
	}

	private void orientActivity() {
		Log.v(CLASS_NAME, "orientActivity()");
		if (getResources().getConfiguration().orientation != getConfigOrientationPref()) {
			setRequestedOrientation(getScreenOrientationPref());
		}
	}

	private CountDownTimer startCapturePlayCountdown(int timeoutInSecs) {
		final TextView countdownDisplay = (TextView) findViewById(R.id.captureCountdown);
		return new CountDownTimer(timeoutInSecs * 1000, 1000) {
			public void onTick(long millisUntilFinished) {
				countdownDisplay.setText("" + (millisUntilFinished / 1000));
			}

			public void onFinish() {
				startCapturePlayback();
			}
		}.start();
	}
	
	private void startCapturePlayback() {
		findViewById(R.id.captureCountdown).setVisibility(View.GONE);
		findViewById(R.id.captureDialogButtonSection).setVisibility(
				View.GONE);
		mCurrentIntentReqIndex = 0;
		sendNextIntent();
	}
	
	private void sendNextIntent() {
		Log.v(CLASS_NAME, "sendNextIntent(): currentIndex = "
				+ mCurrentIntentReqIndex + ", videoList = " + mVideoList.size());
		int numVideos = mVideoList.size();
		final TextView msgArea = (TextView) findViewById(R.id.captureMgrMsg);

		if (mCurrentIntentReqIndex < numVideos) {
			final Intent videoTrainingIntent = new Intent(this,
					Recorder.class).putExtra(Constants.EXTRA_VIDEO_ITEM,
					mVideoList.get(mCurrentIntentReqIndex));
			if (mCurrentIntentReqIndex == 0) {
				startActivityForResult(videoTrainingIntent,
						VIDEO_CAPTURE_ACTIVITY_REQ_CODE);
			} else {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						startActivityForResult(videoTrainingIntent,
								VIDEO_CAPTURE_ACTIVITY_REQ_CODE);
					}
				}, mIntraListInterval * 1000);
				msgArea.setText(getResources().getString(
						R.string.capture_in_progress_msg)
						+ ". Waiting "
						+ mIntraListInterval
						+ " seconds before playing video "
						+ (mCurrentIntentReqIndex + 1) + " of " + numVideos);
			}
		} else {
			msgArea.setText("Playback completed");
			Log.d(CLASS_NAME, "all done: " + numVideos + ","
					+ mCurrentIntentReqIndex + ", " + mSuccessfulPlaybacks);
			// if we have at least one successful auto playback
			// increment the list played count and schedule the next
			if (mSuccessfulPlaybacks > 0) {
				if (mIsAutoPlay) {
					Context context = getApplicationContext();
					VideoList list = ((AVRecorderApp) context).getCurrentList();
					list.incAutoPlayCount(context);
					startService(new Intent(context,
							PlayVideosIS.class)
							.setAction(PlayVideosIS.ACTION_AUTO_VIDEO_LIST_PLAY));
				}
				finishThisActivity(RESULT_OK);
			} else {
				Log.w(CLASS_NAME,
						"TODO: probably no videos were actually playable?");// TODO
			}
		}
	}

	private void postponeAutoPlay() {
		startService(new Intent(getApplicationContext(),
				PlayVideosIS.class)
				.setAction(PlayVideosIS.ACTION_POSTPONED_VIDEO_LIST_PLAY));
		finishThisActivity(RESULT_CANCELED);
	}

	private void finishThisActivity(int result) {
		setResult(result);
		finish();
	}

	@Override
	public void onClick(View v) {
		Log.v(CLASS_NAME, "onClick(): " + v);
		switch (v.getId()) {
		case R.id.autoPlayPostponeButton:
			if (mPlaybackStartCountDown != null) {
				mPlaybackStartCountDown.cancel();
			}
			postponeAutoPlay();
			break;
		case R.id.autoPlayStartButton:
			if (mPlaybackStartCountDown != null) {
				mPlaybackStartCountDown.cancel();
				startCapturePlayback();
			}
			break;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		((AVRecorderApp) getApplication()).setCaptureCompleted();
	}

}
