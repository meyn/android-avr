package msc.meyn.avr.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebViewClient;

import java.util.ArrayList;

import msc.meyn.avr.Constants;
import msc.meyn.avr.R;
import msc.meyn.avr.browser.MeynWebChromeClient;
import msc.meyn.avr.browser.MeynWebView;
import msc.meyn.avr.support.VideoItem;

public class Training extends Activity {

	private final String LOGTAG = "TrainingActivity";
	private SharedPreferences mSharedPrefs = null;
	private MeynWebView mWebView = null;

	private VideoItem mCurrentVideo;
	private ArrayList<MotionEvent> mCaptureEvents = new ArrayList<MotionEvent>();
	private boolean mIsMuted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);

		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.setRequestedOrientation(getScreenOrientationPref());

		Bundle extras = getIntent().getExtras();
		mCurrentVideo = extras.getParcelable(Constants.EXTRA_VIDEO_ITEM);
		Log.d(LOGTAG, "currentVideo = " + mCurrentVideo);
		initWebView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.app_capture_activity_menu_, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOGTAG, "onOptionsItemSelected(): " + item.getItemId() + ", "
				+ R.menu.app_init_);
		switch (item.getItemId()) {
		case R.id.action_copyurl:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("VCrawlerURL",
					mCurrentVideo.getUrl());
			clipboard.setPrimaryClip(clip);
			break;
		case R.id.action_mute_unmute:
			AudioManager audioManager = (AudioManager) this
					.getSystemService(Context.AUDIO_SERVICE);
			mIsMuted = !mIsMuted;
			audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mIsMuted);
			break;
		}
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		mWebView.loadUrl(mCurrentVideo.getUrl());
		new InstructionsDialogFragment().show(getFragmentManager(),
				"InstructionsDialog");
		capturePlayClick();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(LOGTAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		Log.v(LOGTAG, "onBackPressed(): " + mWebView.getScrollX() + ", "
				+ mWebView.getScrollY() + " --> " + mCaptureEvents);
		mCurrentVideo.getSite().writeAutoPlayData(
				getConfigOrientationPref() - 1, mWebView.getScrollX(),
				mWebView.getScrollY(), mCaptureEvents, this);
		captureDone();
	}

	@Override
	public void onStop() {
		super.onStop();
		captureDone();
	}

	private int getScreenOrientationPref() {
		String value = mSharedPrefs.getString("pref_key_video_oriention_value",
				"2").trim();
		Log.d(LOGTAG, "getOrientationPref(): " + value);
		if (value.equals("1")) {
			return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;// Configuration.ORIENTATION_PORTRAIT;
		}
		return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;// Configuration.ORIENTATION_LANDSCAPE;
	}

	private int getConfigOrientationPref() {
		return (getScreenOrientationPref() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initWebView() {
		mWebView = (MeynWebView) findViewById(R.id.meyn_webview);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebChromeClient(new MeynWebChromeClient(null));
		mWebView.setWebViewClient(new WebViewClient());
		mWebView.getSettings().setPluginState(PluginState.OFF);
		mWebView.clearCache(true);
	}

	private void capturePlayClick() {
		Log.d(LOGTAG, "capturePlayClick()");
		mCaptureEvents.clear();
		mWebView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, final MotionEvent event) {
				mCaptureEvents.add(MotionEvent.obtain(event));
				return false;
			}
		});
	}

	private void captureDone() {
		Log.d(LOGTAG, "captureDone()");
		mCaptureEvents.clear();
		mWebView.loadUrl("about:blank");
		finish();
	}

	public static class InstructionsDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("1. Wait for the page to load.\n"
					+ "2. Click on the screen to start video playback.\n"
					+ "3. Press the back button anytime after the video starts.\n"
					+ "-- To re-train a site first delete its simulation data from the app settings menu --\n\n"
					+ "Press the back button to dismiss this dialog");
			return builder.create();
		}
	}

}
