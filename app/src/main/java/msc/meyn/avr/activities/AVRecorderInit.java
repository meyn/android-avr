package msc.meyn.avr.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.R;
import msc.meyn.avr.services.PlayVideosIS;
import msc.meyn.avr.services.UploadFileFTPIS;
import msc.meyn.avr.support.VideoItem;
import msc.meyn.avr.support.VideoList;

public class AVRecorderInit extends Activity implements OnClickListener {

	private static final String CLASS_NAME = AVRecorderInit.class.getName();

	private static final int TRAINING_PROMPT_ACTIVITY_REQ_CODE = 1;
	private static final int PLAYBACK_CONTROLLER_ACTIVITY_REQ_CODE = 2;

	private ProgressDialog mProgressDialog;

	private final static String ACTION_INTENT_UPLOAD_COMPLETE = CLASS_NAME
			+ ".ACTION_INTENT_UPLOAD_COMPLETE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_app_init);

		findViewById(R.id.uploadFiles_btn).setOnClickListener(this);
		findViewById(R.id.browseFiles_btn).setOnClickListener(this);
		findViewById(R.id.fetch_playlist_btn).setOnClickListener(this);
		findViewById(R.id.startPlaybackNow_btn).setOnClickListener(this);
		findViewById(R.id.startTraining_btn).setOnClickListener(this);
		findViewById(R.id.verifyTraining_btn).setOnClickListener(this);

		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setIndeterminate(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		VideoList list = ((AVRecorderApp) getApplication()).getCurrentList();

		if (list != null) {
			ArrayList<VideoItem> allVideos = list.getAllVideos();
			ArrayList<VideoItem> trainedVideos = list
					.getTrainedVideoItems(this);
			if (allVideos.size() > 0) {
				findViewById(R.id.startTraining_btn).setVisibility(
						(allVideos.size() == trainedVideos.size() ? View.GONE
								: View.VISIBLE));
				if (trainedVideos.size() < 1) {
					startActivityForResult(new Intent(this,
							TrainingController.class),
							TRAINING_PROMPT_ACTIVITY_REQ_CODE);
				}
			}
		} else {
			startActivity(new Intent(this, VideoListBrowser.class));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.startPlaybackNow_btn:
			sendPlayIntent(false);
			break;
		case R.id.startTraining_btn:
			sendTrainingIntent();
			break;
		case R.id.verifyTraining_btn:
			sendPlayIntent(true);
			break;
		case R.id.fetch_playlist_btn:
			startActivity(new Intent(this, VideoListBrowser.class));
			break;
		case R.id.browseFiles_btn:
			viewLogFolder();
			break;
		case R.id.uploadFiles_btn:
			uploadCaptures();
			break;
		default:
			Log.e(CLASS_NAME, "no click handler defined: " + v);
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.app_init_, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			startActivity(new Intent(this, AVRecorderAbout.class));
			break;
		case R.id.action_settings:
			startActivity(new Intent(this, Preferences.class));
			break;
		}
		return true;
	}

	private void showMsg(String msg) {
		Log.d(CLASS_NAME, msg);
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(CLASS_NAME, "onActivityResult(): " + requestCode + ", "
				+ resultCode + ", " + data);
		switch (requestCode) {
		case TRAINING_PROMPT_ACTIVITY_REQ_CODE:
			showMsg("TRAINING_PROMPT_ACTIVITY_REQ_CODE result: " + resultCode);
			break;
		case PLAYBACK_CONTROLLER_ACTIVITY_REQ_CODE:
			showMsg("PLAYBACK_CONTROLLER_ACTIVITY_REQ_CODE result: "
					+ resultCode);
			break;
		}
	}

	private void sendPlayIntent(boolean verify) {
		VideoList list = ((AVRecorderApp) getApplication()).getCurrentList();
		if (list == null || list.getTrainedVideoItems(this).size() < 1) {
			showMsg("No playable videos yet. AVRecorder needs to be trained after downloading a video list.");
		} else {
			Intent playIntent = new Intent(this, RecorderController.class)
					.setAction(PlayVideosIS.ACTION_MANUAL_VIDEO_LIST_PLAY)
					.putExtra(PlayVideosIS.EXTRA_VERFIY, verify);
			startActivityForResult(playIntent,
					PLAYBACK_CONTROLLER_ACTIVITY_REQ_CODE);
		}
	}

	private void sendTrainingIntent() {
		VideoList list = ((AVRecorderApp) getApplication()).getCurrentList();
		if (list != null) {
			ArrayList<VideoItem> untrainedVideos = list
					.getUntrainedVideoItems(this);
			if (untrainedVideos.size() > 0) {
				startActivityForResult(new Intent(this,
						TrainingController.class),
						TRAINING_PROMPT_ACTIVITY_REQ_CODE);
			} else {
				showMsg("No video sites appear to require training for automatic playback at this time.\n"
						+ "Try testing the trained sites");
			}
		} else {
			showMsg("Please download a video list");
		}
	}

	private void viewLogFolder() {
		Uri startDir = Uri.fromFile(Environment
				.getExternalStoragePublicDirectory(this.getPackageName() + "/"
						+ Constants.LOCAL_LOG_DIR_NAME + "/"));
		try {
			Intent fileManagerIntent = new Intent(Intent.ACTION_VIEW);
			fileManagerIntent.setDataAndType(startDir, "resource/folder");
			startActivity(fileManagerIntent);
		} catch (ActivityNotFoundException e) {
			showMsg("Unable to display files/folders. Storage location: "
					+ startDir);
		}
	}

	private void uploadCaptures() {
		String baseDir = Environment.getExternalStoragePublicDirectory(
				this.getPackageName() + "/" + Constants.LOCAL_LOG_DIR_NAME)
				.getAbsolutePath();
		ArrayList<String> files = new ArrayList<String>();
		File root = new File(baseDir);
		File captureFiles[];
		final class FilteredFiles implements FilenameFilter {
			final String suffix;

			FilteredFiles(String suffix) {
				this.suffix = suffix;
			}

			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(suffix);
			}
		}
		mProgressDialog.setMessage("Determining files...");
		captureFiles = root.listFiles(new FilteredFiles(".zip"));
		for (File file : captureFiles) {
			files.add(file.getName());
		}

		if (files.size() > 0) {
			Intent ftpUploadIntent = new Intent(this, UploadFileFTPIS.class)
					.putExtra(UploadFileFTPIS.EXTRA_KEY_RESP_ACTION_INTENT,
							ACTION_INTENT_UPLOAD_COMPLETE)
					.putExtra(UploadFileFTPIS.EXTRA_KEY_LOCAL_BASE_DIR, baseDir)
					.putExtra(UploadFileFTPIS.EXTRA_KEY_REMOTE_LOG_DIR,
							Constants.REMOTE_LOG_DIR_NAME)
					.putStringArrayListExtra(
							UploadFileFTPIS.EXTRA_KEY_FILES_LIST, files);

			// register to receive the upload completion event
			LocalBroadcastManager.getInstance(this).registerReceiver(
					mBroadcastReceiver,
					new IntentFilter(ACTION_INTENT_UPLOAD_COMPLETE));
			startService(ftpUploadIntent);

			if (!mProgressDialog.isShowing()) {
				mProgressDialog.setMessage("Uploading ...");
				mProgressDialog.show();
			}
		} else {
			showMsg("No captures to upload.");
		}
	}

	private void handleUploadResponse(boolean success) {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mBroadcastReceiver);
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(CLASS_NAME, "onReceive(): " + context + ", " + intent + ", "
					+ intent.getAction());
			if (intent.getAction().equals(ACTION_INTENT_UPLOAD_COMPLETE)) {
				Bundle extras = intent.getExtras();
				Log.d(CLASS_NAME, intent.getAction() + ": " + extras);
				boolean uploadSuccess = false;
				if (extras != null) {
					uploadSuccess = extras
							.getBoolean(
									UploadFileFTPIS.EXTRA_KEY_COMPLETION_SUCCESS,
									false);
				}
				handleUploadResponse(uploadSuccess);
			}
		}
	};

}
