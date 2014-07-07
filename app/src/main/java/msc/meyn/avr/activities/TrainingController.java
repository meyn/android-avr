package msc.meyn.avr.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.R;
import msc.meyn.avr.services.PlayVideosIS;
import msc.meyn.avr.support.VideoItem;
import msc.meyn.avr.support.VideoList;

public class TrainingController extends Activity {

	private static final String CLASS_NAME = TrainingController.class.getName();

	public static final String EXTRA_KEY_TIMEOUT_SECS = CLASS_NAME + ".EXTRA_KEY_TIMEOUT_SECS";
	
	private static final String KEY_INTENT_INDEX = "CURRENT_INDEX";
	private static final String KEY_INTENT_VIDEO_LIST = "VIDEO_LIST";
	private static final int TRAINING_INTENT_REQ_CODE = 1;

	private int mListCurrentIndex;
	private ArrayList<VideoItem> mVideoList;

	private CountDownTimer mTrainingStartCountDown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (((AVRecorderApp) getApplication()).setTrainingInProgress() == false) {
			setResult(RESULT_CANCELED);
			finish();
			Log.e(CLASS_NAME, "CANCELLING due to conflict");
			return;
		}

		this.setFinishOnTouchOutside(false);
		setContentView(R.layout.dialog_activity_training_controller);
		
		Bundle extras = getIntent().getExtras();

		final Button noButton = (Button) findViewById(R.id.prompt_no_btn);
		final Button yesButton = (Button) findViewById(R.id.prompt_yes_btn);

		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				VideoList list = ((AVRecorderApp) getApplication())
						.getCurrentList();
				if (list != null) {
					startTraining(list);
				} else {
					Log.e(CLASS_NAME, "video list is null");
					finishThisActivity(RESULT_CANCELED);
				}
			}
		});

		noButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishThisActivity(RESULT_CANCELED);
			}
		});

		if (((AVRecorderApp) getApplication()).getCurrentList()
				.getTrainedVideoItems(this).size() < 1) {
			noButton.setVisibility(View.GONE);
			findViewById(R.id.trainingMgrCountdown).setVisibility(View.GONE);
		} else {
			int coutdownValue = 10;
			if (extras != null) {
				coutdownValue = extras.getInt(EXTRA_KEY_TIMEOUT_SECS, 10);
			}
			mTrainingStartCountDown = startCapturePlayCountdown(coutdownValue);
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					noButton.setVisibility(View.VISIBLE);
				}
			}, 30 * 1000);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_INTENT_INDEX, mListCurrentIndex);
		outState.putParcelableArrayList(KEY_INTENT_VIDEO_LIST, mVideoList);
		Log.d(CLASS_NAME, "onSaveInstanceState(): " + outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mListCurrentIndex = savedInstanceState.getInt(KEY_INTENT_INDEX);
		mVideoList = savedInstanceState
				.getParcelableArrayList(KEY_INTENT_VIDEO_LIST);
		Log.d(CLASS_NAME, "onRestoreInstaceState(): " + savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(CLASS_NAME, "onActivityResult(): " + requestCode + ", "
				+ resultCode + ", " + data);
		switch (requestCode) {
		case TRAINING_INTENT_REQ_CODE:
			++mListCurrentIndex;
			sendNextTrainingIntent();
			break;
		}
	}

	@Override
	public void onBackPressed() {
		Log.v(CLASS_NAME, "onBackPressed()");
	}

	private CountDownTimer startCapturePlayCountdown(int timeoutInSecs) {
		final TextView countdownDisplay = (TextView) findViewById(R.id.trainingMgrCountdown);
		return new CountDownTimer(timeoutInSecs * 1000, 1000) {
			public void onTick(long millisUntilFinished) {
				countdownDisplay.setText("Cancelling in "
						+ (millisUntilFinished / 1000));
			}

			public void onFinish() {
				finishThisActivity(RESULT_CANCELED);
			}
		}.start();
	}

	private void startTraining(VideoList list) {
		if (mTrainingStartCountDown != null) {
			mTrainingStartCountDown.cancel();
		}
		mListCurrentIndex = 0;
		mVideoList = list.getUntrainedVideoItems(getApplicationContext());
		sendNextTrainingIntent();
	}

	private void sendNextTrainingIntent() {
		Log.v(CLASS_NAME, "sendNextIntent(): videos = " + mVideoList.size()
				+ ", currentIndex = " + mListCurrentIndex);
		if (mListCurrentIndex < mVideoList.size()) {
			VideoItem video = mVideoList.get(mListCurrentIndex);
			Intent videoPlaybackIntent = new Intent(this,
					Training.class);
			videoPlaybackIntent.putExtra(Constants.EXTRA_VIDEO_ITEM, video);
			startActivityForResult(videoPlaybackIntent,
					TRAINING_INTENT_REQ_CODE);
		} else {
			finishThisActivity(RESULT_OK);
		}
	}

	private void finishThisActivity(int result) {
		Log.v(CLASS_NAME, "finishThisActivity(): " + result);
		// on completion check if we have at least 1 playable video site
		Context context = getApplicationContext();
		VideoList list = ((AVRecorderApp) context).getCurrentList();
		if (list != null) {
			ArrayList<VideoItem> autoPlayableVideos = list
					.getTrainedVideoItems(context);
			if (autoPlayableVideos.size() < 1) {
				Toast.makeText(context, "No sites trained, trying again.",
						Toast.LENGTH_SHORT).show();
				startTraining(list);
				return;
			}
		}
		((AVRecorderApp) getApplication()).setTrainingCompleted();
		startService(new Intent(context, PlayVideosIS.class)
				.setAction(PlayVideosIS.ACTION_AUTO_VIDEO_LIST_PLAY));
		setResult(result);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		((AVRecorderApp) getApplication()).setTrainingCompleted();
	}

}
