package msc.meyn.avr.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.R;
import msc.meyn.avr.browser.MeynVideoJsInterface;
import msc.meyn.avr.browser.MeynWebChromeClient;
import msc.meyn.avr.browser.MeynWebView;
import msc.meyn.avr.recorder.support.EventLogger;
import msc.meyn.avr.recorder.support.TcpdumpHandler;
import msc.meyn.avr.recorder.support.ZipFilesTask;
import msc.meyn.avr.services.UploadFileFTPIS;
import msc.meyn.avr.support.VideoItem;
import msc.meyn.avr.support.VideoSiteAutoPlayData;

public class Recorder extends Activity implements EventLogger.LoggerEvents, TcpdumpHandler.Listener, MeynVideoJsInterface.JsEvents {

	private VideoItem mCurrentVideo;
	private String mLogDirPath;
	private EventLogger mEventLogger;
	private TcpdumpHandler mTcpdumpHandler;
	private String mFilePrefix;
	private int mPageSaveCounter = 0;

	private final Handler mVideoTimeoutHandler = new Handler();
	private Runnable mOnVideoTimeout = new Runnable() {
		@Override
		public void run() {
			updateState(STATE.PLAYBACK_TIMEDOUT, true);
		}
	};

	private SSListener mSigStateListener;

	private SharedPreferences mSharedPrefs = null;
	private MeynWebView mWebView = null;
	
	private ProgressDialog mProgressDialog;
	private boolean mIsMuted = false;
	
	private enum PHASE {
		BEGIN, VIDEO_SETUP, PLAY_VIDEO, END
	}

	private enum STATE {
		ORIENTATION_SET, LOGGING_STARTED, LOGGING_ENDED, TCPDUMP_STARTED, TCPDUMP_ENDED, PAGE_LOADED, PAGE_SAVED, READY_FOR_VIDEO, PLAY_CLICKED, PLAYBACK_TIMEDOUT, PLAYBACK_ENDED, ZIPPING_ENDED, UPLOAD_ENDED
	};

	private PHASE mPreviousPhase, mCurrentPhase;
	private Map<STATE, Boolean> stateMap = new EnumMap<STATE, Boolean>(
			STATE.class);

	private final static String CLASS_NAME = Recorder.class.getSimpleName();
	private final static String ACTION_INTENT_UPLOAD_COMPLETE = CLASS_NAME + ".ACTION_INTENT_UPLOAD_COMPLETE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mCurrentVideo = extras.getParcelable(Constants.EXTRA_VIDEO_ITEM);
			if (mCurrentVideo != null) {
				setRequestedOrientation(getScreenOrientationPref());
				setContentView(R.layout.activity_webview);

				mFilePrefix = mCurrentVideo.createFilePrefix();
				mLogDirPath = Environment.getExternalStoragePublicDirectory(
						this.getPackageName() + "/"
								+ Constants.LOCAL_LOG_DIR_NAME)
						.getAbsolutePath();
				mTcpdumpHandler = new TcpdumpHandler(this,
						Constants.TCPDUMP_EXE_NAME, this.getFilesDir()
								.getPath(), 0, mLogDirPath + "/" + mFilePrefix
								+ ".pcap");
				mWebView = (MeynWebView) findViewById(R.id.meyn_webview);
				initWebView();
				mProgressDialog = new ProgressDialog(this);
				mProgressDialog.setIndeterminate(true);
				mProgressDialog.setTitle("Please wait");

				if (isEnvironmentReady()) {
					for (STATE entry : STATE.values()) {
						stateMap.put(entry, false);
					}
					AudioManager audioManager = (AudioManager) this
							.getSystemService(Context.AUDIO_SERVICE);
					audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
					mIsMuted = true;
					moveToPhase(PHASE.BEGIN);
				} else {
					finishWithError("Unable to start capture");
				}
			}
		} else {
			finishWithError("Invalid video");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.app_capture_activity_menu_, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(CLASS_NAME, "onOptionsItemSelected(): " + item.getItemId() + ", "
				+ R.menu.app_init_);
		switch (item.getItemId()) {
		case R.id.action_copyurl:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
			ClipData clip = ClipData.newPlainText("VCrawlerURL", mCurrentVideo.getUrl());
			clipboard.setPrimaryClip(clip);
			break;
		case R.id.action_mute_unmute:
			AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			mIsMuted = !mIsMuted;
			audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mIsMuted);
			break;
		}
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(CLASS_NAME, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onBackPressed() {
		logEvent("Activity", "onBackPressed()");
		moveToPhase(PHASE.END);
	}

	@Override
	public void onPause() {
		logEvent("Activity", "onPause");
		super.onPause();
	}

	@Override
	public void onStop() {
		logEvent("Activity", "onStop");
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(CLASS_NAME, "onDestroy()");
		mWebView.destroy();
		AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
		super.onDestroy();
	}

	private void finishWithError(String errorMessage) {
		Log.e(CLASS_NAME, "finishWithError(): " + errorMessage);
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
		finishThisActivity(RESULT_CANCELED);
	}

	private void finishThisActivity(int result) {
		setResult(result);
		finish();
	}

	private boolean isEnvironmentReady() {
		return (((AVRecorderApp)getApplication()).isOnline()
				&& Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				&& mCurrentVideo.getSite().isAutoPlayable(getConfigOrientationPref()-1, this));
	}

	private void initWebView() {
		mWebView.addJavascriptInterface(new MeynVideoJsInterface(this),
				MeynVideoJsInterface.JS_API_NAME);
		mWebView.setWebChromeClient(new MeynWebChromeClient(this));
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.d(CLASS_NAME, "onPageStarted(): " + url);
			}

			@Override
			public void onPageFinished(final WebView view, String url) {		
				updateState(STATE.PAGE_LOADED, true);
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Log.e(CLASS_NAME, "onReceivedError(): " + errorCode + ", " + description + ", " + failingUrl);
				updateState(STATE.PAGE_LOADED, false);
			}
		});
		mWebView.clearCache(true);
	}

	public void clearWebView() {
		mWebView.stopLoading();
		mWebView.loadUrl("about:blank");
		mWebView.loadData("<html><body>AVRecorder</body></html>", "text/html", "utf-8");
		mWebView.removeJavascriptInterface(MeynVideoJsInterface.JS_API_NAME);
	}

	private double getTimeoutDuration() {
		double maxVideoDuration = mCurrentVideo.getDuration() * 2;
		int timeoutVal = Integer.parseInt(mSharedPrefs.getString(
				"pref_key_video_duration_value", "420"));
		if (timeoutVal < 0 || timeoutVal > Constants.MAX_VIDEO_DURATION_SECS) {
			timeoutVal = Constants.DEFAULT_VIDEO_DURATION_SECS;
		}
		if (maxVideoDuration > timeoutVal) {
			maxVideoDuration = timeoutVal;
		}
		return maxVideoDuration;
	}
	
	/**
	 * Inject JavaScript code to hook into the <video> player events
	 */
	private void injectVideoEventListeners() {
		mWebView.loadUrl("javascript:(function() {"
				+ 	"console.log('this is a test msg');"
				+ 	"var logMsg = function(msg) {"
				+ 		"window." + MeynVideoJsInterface.JS_API_NAME + ".log(msg);"
				+ 	"};"
				+ 	"var bodyElem = document.querySelector('body');"
				+ 	"var videoElem = document.querySelector('video');"
				+	"var videoEvents = ['abort','canplay','canplaythrough',"
				+ 		"'durationchange','emptied','error','loadeddata',"
				+ 		"'loadedmetadata','loadstart','pause','play','playing',"
				+ 		"'progress','ratechange','resize','seeked','seeking',"
				+ 		"'stalled','suspend','volumechange','waiting'];"
				+ 	"var setupVideoEventListeners = function(elem) {"
				+ 		"logMsg('video element: ' + elem.outerHTML);"
				+ 		"for (var eventIndex in videoEvents) {"
				+ 			"elem.addEventListener(videoEvents[eventIndex], function(e) {"
				+ 				"e = e || window.event;"
				+ 				"var el = e.target;"
				+ 				"var str = e.type + ':{duration:' + el.duration + ', currentTime:' + el.currentTime + '}';"
				+ 				"logMsg(str);"
				+ 			"}, false);"
				+ 		"}"
				+		"elem.addEventListener('timeupdate', function(e) {"
				+			"e = e || window.event;"
				+ 			"var el = e.target;"
				+ 			"logMsg(e.type + ':{currentTime:' + el.currentTime + '}');"
				+ 		"}, false);"
				+ 		"elem.addEventListener('ended', function(e) {" // handle ended event separately
				+ 			"e = e || window.event;"
				+ 			"var el = e.target;"
				+ 			"logMsg(e.type + ' {duration:' + el.duration + ', currentTime:' + el.currentTime + '}');"
				+			"window." + MeynVideoJsInterface.JS_API_NAME + ".onPlaybackEnded(el.currentTime);"
				+ 		"}, false);"
				+ 	"};"
				+ 	"var nodeInsertedListener = function(e) {"
				+ 		"e = e || window.event;"
				+ 		"if (e.target.tagName === 'VIDEO') {"
				+ 			"setupVideoEventListeners(e.target);"
				+ 			"bodyElem.removeEventListener('DOMNodeInserted', nodeInsertedListener, false);"
				+ 		"}"
				+ 	"};"
				+ 	"if (videoElem) {"
				+ 		"setupVideoEventListeners(videoElem);"
				+ 	"} else {"
				+ 		"bodyElem.addEventListener('DOMNodeInserted', nodeInsertedListener, false);"
				+ 	"}"
				+	"setTimeout(function() {window." + MeynVideoJsInterface.JS_API_NAME + ".readyToPlay();}, 0);"
				+ "})();");
	}

	/**
	 * 
	 * @param prefixTag
	 * @param msg
	 */
	private void logEvent(String src, String eventMsg) {
		if (mEventLogger != null) {
			mEventLogger.logEvent(src, eventMsg);
		} else {
			Log.w(CLASS_NAME, "logEvent(): [" + src + "], " + eventMsg);
		}
	}

	// has to be run on the UI thread
	private void simulatePlayClick() {
		final VideoSiteAutoPlayData autoPlayData = mCurrentVideo
				.getSite()
				.getAutoPlayEventData((getConfigOrientationPref() - 1), this);
		Log.d(CLASS_NAME, "simulatePlayClick(): events: " + autoPlayData);
		logEvent("Activity", "simulatePlayClick():" + autoPlayData);
		if ((Looper.myLooper() == Looper.getMainLooper()) && (autoPlayData != null)) {
			mWebView.scrollBy(autoPlayData.getScrollX(), autoPlayData.getScrollY());
			final long currentTime = SystemClock.uptimeMillis();
			final MotionEvent.PointerProperties ptrProps[] = new MotionEvent.PointerProperties[1];
			final MotionEvent.PointerCoords[] ptrCoords = new MotionEvent.PointerCoords[1];
			ptrProps[0] = new MotionEvent.PointerProperties();
			ptrProps[0].id = 0;
			ptrProps[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
			ptrCoords[0] = new MotionEvent.PointerCoords();
			ptrCoords[0].pressure = 1.0f;
			ptrCoords[0].size = 0.04f;
			for (VideoSiteAutoPlayData.AutoPlayMotionEvent autoEvent : autoPlayData.getEvents()) {
				ptrCoords[0].x = autoEvent.x;
				ptrCoords[0].y = autoEvent.y;
				mWebView.dispatchTouchEvent(MotionEvent.obtain(currentTime,
						SystemClock.uptimeMillis(), autoEvent.action, 1, ptrProps, ptrCoords, 0, 0,
						1.0f, 1.0f, autoEvent.deviceId, 0, autoEvent.source, 0));
			}
			updateState(STATE.PLAY_CLICKED, true);
		} else {
			updateState(STATE.PLAY_CLICKED, false);
		}
	}

	private int getScreenOrientationPref() {
		String value = mSharedPrefs.getString("pref_key_video_oriention_value",
				"2").trim();
		Log.d(CLASS_NAME, "getOrientationPref(): " + value);
		if (value.equals("1")) {
			return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;// Configuration.ORIENTATION_PORTRAIT;
		}
		return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;//Configuration.ORIENTATION_LANDSCAPE;
	}

	private int getConfigOrientationPref() {
		return (getScreenOrientationPref() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE);
	}

	private void zipUpFiles() {
		Log.d(CLASS_NAME, "zipUpFiles()");
		if (!mProgressDialog.isShowing()) {
			mProgressDialog.setMessage("Compressing capture...");
			mProgressDialog.show();
		}
		new ZipFilesTask(new ZipFilesTask.TaskNotifier() {
			@Override
			public void onZipCompleted(String result) {
				Log.v(CLASS_NAME, "onZipCompleted(): " + result);
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				updateState(STATE.ZIPPING_ENDED, (result != null));
			}
			@Override
			public void onZipError(String errMsg) {
				Log.v(CLASS_NAME, "onZipError(): " + errMsg);
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				updateState(STATE.ZIPPING_ENDED, false);
			}
			@Override
			public void onZipProgress(String msg) {
				Log.v(CLASS_NAME, "onZipProgress(): " + msg);
			}
		},
		mLogDirPath,
		mFilePrefix).execute(mFilePrefix + ".zip");
	}
	
	private void uploadZipFile() {
		Log.d(CLASS_NAME, "uploadFile()");
		if (mSharedPrefs.getBoolean("pref_key_immediate_upload", true)) {
			ArrayList<String> files = new ArrayList<String>();
			files.add(mFilePrefix + ".zip");
			// register to receive the upload completion event
			if (files.size() > 0) {
				Intent ftpUploadIntent = new Intent(this, UploadFileFTPIS.class)
						.putExtra(UploadFileFTPIS.EXTRA_KEY_RESP_ACTION_INTENT,
								ACTION_INTENT_UPLOAD_COMPLETE)
						.putExtra(UploadFileFTPIS.EXTRA_KEY_LOCAL_BASE_DIR,
								mLogDirPath)
						.putExtra(UploadFileFTPIS.EXTRA_KEY_REMOTE_LOG_DIR,
								Constants.REMOTE_LOG_DIR_NAME)
						.putStringArrayListExtra(
								UploadFileFTPIS.EXTRA_KEY_FILES_LIST, files);
				LocalBroadcastManager.getInstance(this).registerReceiver(
						mBroadcastReceiver,
						new IntentFilter(ACTION_INTENT_UPLOAD_COMPLETE));
				startService(ftpUploadIntent);
				if (!mProgressDialog.isShowing()) {
					mProgressDialog.setMessage("Uploading capture...");
					mProgressDialog.show();
				}
			}
		} else {
			updateState(STATE.UPLOAD_ENDED, false);
		}
	}
	
	private void onUploadCompleted(boolean success) {
		Log.v(CLASS_NAME, "handleUploadResponse(): " + success);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		updateState(STATE.UPLOAD_ENDED, success);
	}

	public String createFilePrefix() {
		Calendar cal = Calendar.getInstance();
		cal.getTimeInMillis();
		return mCurrentVideo.getId() + "_"
				+ String.format("%ty%tm%td%tH%tM%tS", cal, cal, cal, cal, cal,
						cal);
	}

	private void registerConnectivityListeners() {
		// network connectivity change events
		registerReceiver(mBroadcastReceiver,
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		// mobile connection strength changes
		mSigStateListener = new SSListener();
		((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(mSigStateListener,
				PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}
	
	private void unregisterConnectivityListeners() {
		unregisterReceiver(mBroadcastReceiver);
		if (mSigStateListener != null) {
			((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(mSigStateListener,
					PhoneStateListener.LISTEN_NONE);
		}
	}
	
	private void startLogging() {
		try {
			mEventLogger = new EventLogger(this, this, new File(mLogDirPath, mFilePrefix
					+ ".log"));
			mEventLogger.start();
			mEventLogger.writeListInfo(((AVRecorderApp)getApplication()).getCurrentList());
			mEventLogger.writeVideoInfo(mCurrentVideo.toString());
			mEventLogger.logWifiRssi();
			mEventLogger.writeSysStats();
			registerConnectivityListeners();
		} catch (Exception e) {
			e.printStackTrace();
			updateState(STATE.LOGGING_STARTED, false);
		}
	}

	private void stopLogging() {
		unregisterConnectivityListeners();
		mEventLogger.writeSysStats();
		mEventLogger.stop();
	}
	
	private void updateState(STATE state, boolean succeeded) {
		Log.d(CLASS_NAME, "updateState(): " + mCurrentPhase + ": " + state + ", "
				+ succeeded);

		logEvent("Activity", mCurrentPhase + " " + state + " : "
				+ succeeded);

		switch (mCurrentPhase) {
		case BEGIN:
			switch (state) {
			case ORIENTATION_SET:
				startLogging();
				return;
			case LOGGING_STARTED:
				if (!succeeded) {
					moveToPhase(PHASE.END);
				} else {
					if (!mProgressDialog.isShowing()) {
						mProgressDialog.setMessage("Starting tcpdump...");
						mProgressDialog.show();
					}
					mTcpdumpHandler.startTcpdump();
				}
				return;
			case TCPDUMP_STARTED:
				if (!succeeded && !(mSharedPrefs.getBoolean("pref_key_continue_without_tcpdump", true))) {
					moveToPhase(PHASE.END);
				} else {
					moveToPhase(PHASE.VIDEO_SETUP);			
				}
				return;
			default:
				Log.e(CLASS_NAME, mCurrentPhase + " - " + state + ", " + succeeded);
			}
			break;
			
		case VIDEO_SETUP:
			switch (state) {
			case PAGE_LOADED:
				if (succeeded) {
					requestPageSave();
					injectVideoEventListeners();
				} else {
					moveToPhase(PHASE.END);
				}
				return;
			case PAGE_SAVED:
			case READY_FOR_VIDEO:
				stateMap.put(state, succeeded);
				if (stateMap.get(STATE.PAGE_SAVED) && stateMap.get(STATE.READY_FOR_VIDEO)) {
					moveToPhase(PHASE.PLAY_VIDEO);
				}
				return;
			default:
				Log.e(CLASS_NAME, mCurrentPhase + " - " + state + ", " + succeeded);
			}
			break;
			
		case PLAY_VIDEO:
			switch (state) {
			case PLAY_CLICKED:
				onVideoClicked();
				return;
			case PLAYBACK_TIMEDOUT:
			case PLAYBACK_ENDED:
				moveToPhase(PHASE.END);
				return;
			default:
				Log.e(CLASS_NAME, mCurrentPhase + " - " + state + ", " + succeeded);
			}
			break;
			
		case END:
			switch (state) {
			case TCPDUMP_ENDED:
				stopLogging();
				return;
			case LOGGING_ENDED:
				zipUpFiles();
				return;
			case ZIPPING_ENDED:
				uploadZipFile();
				return;
			case UPLOAD_ENDED:
				finishThisActivity((mPreviousPhase == PHASE.PLAY_VIDEO) ? RESULT_OK : RESULT_CANCELED);
				return;
			default:
				Log.e(CLASS_NAME, mCurrentPhase + " - " + state + ", " + succeeded);
			}
			break;

		default:
			Log.e(CLASS_NAME, "Phase: " + mCurrentPhase + " invalid state: " + state);
			return;
		}
	}

	private void moveToPhase(PHASE newPhase) {
		Log.d(CLASS_NAME, "moveToPhase(): " + newPhase);
		if (newPhase == mCurrentPhase) {
			return;
		}

		logEvent("Activity", mCurrentPhase + " => " + newPhase);
		mPreviousPhase = mCurrentPhase;
		mCurrentPhase = newPhase;
		switch (mCurrentPhase) {
		case BEGIN:
			if (getResources().getConfiguration().orientation != getConfigOrientationPref()) {
				setRequestedOrientation(getScreenOrientationPref());
			}
			updateState(STATE.ORIENTATION_SET, true);
			break;

		case VIDEO_SETUP:
			mWebView.loadUrl(mCurrentVideo.getUrl());
			break;

		case PLAY_VIDEO:
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					simulatePlayClick();
				}
			}, 7*1000); // delay for page rendering
			break;

		case END:
			clearWebView();
			mTcpdumpHandler.stopTcpdump();
			break;

		default:
			break;
		}
	}

	private final class SSListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength strength) {
			logEvent("PhoneStateListener", strength.toString());
		}
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS_NAME, "onReceive(): " + context + ", " + intent + ", "
					+ intent.getAction());
			String action = intent.getAction();
			if (action.equals(ACTION_INTENT_UPLOAD_COMPLETE)) {
				Bundle extras = intent.getExtras();
				Log.d(CLASS_NAME, intent.getAction() + ": " + extras);
				boolean uploadSuccess = false;
				if (extras != null) {
					uploadSuccess = extras.getBoolean(
							UploadFileFTPIS.EXTRA_KEY_COMPLETION_SUCCESS, false);
				}
				onUploadCompleted(uploadSuccess);
			} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				NetworkInfo activeNetwork = ((ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE))
						.getActiveNetworkInfo();
				String status = "no network";
				if (activeNetwork != null) {
					status = activeNetwork.getTypeName();
					logEvent("ConnectivityChange", activeNetwork.toString());
				} else {
					logEvent("ConnectivityChange", status);
				}
			}
		}
	};

	@Override
	public void onLoggingStarted() {
		updateState(STATE.LOGGING_STARTED, true);
	}

	@Override
	public void onLoggingStopped(boolean status) {
		updateState(STATE.LOGGING_ENDED, status);
		mEventLogger = null;
	}

	@Override
	public void onTcpdumpStartError(int exitCode) {
		Log.e(CLASS_NAME, "onTcpdumpStartError(): " +exitCode);
		logEvent("tcpdump", "start error: " + exitCode);
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}		
		updateState(STATE.TCPDUMP_STARTED, false);
	}

	@Override
	public void onTcpdumpStarted() {
		Log.d(CLASS_NAME, "onTcpdumpStarted()");
		logEvent("tpcdump", "started");
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}		
		updateState(STATE.TCPDUMP_STARTED, true);
	}

	@Override
	public void onTcpdumpStopError(int exitCode) {
		logEvent("tcpdump", "stop error: " + exitCode);
		updateState(STATE.TCPDUMP_ENDED, false);
	}

	@Override
	public void onTcpdumpStopped() {
		logEvent("tcpdump", "stopped");
		updateState(STATE.TCPDUMP_ENDED, true);
	}

	@Override
	public void onTcpdumpReport(String msg) {
		logEvent("tcpdump", msg);
	}

	private void requestPageSave() {
		mWebView.loadUrl("javascript:window."
				+ MeynVideoJsInterface.JS_API_NAME
				+ ".saveHTML2File(document.getElementsByTagName('html')[0].outerHTML);");
	}
	
	@Override
	public void onJsLog(String msg) {
		logEvent("JsInterface", msg);
	}

	@Override
	public void onJsSavePage(String source) {
		boolean pageSaved = false;
		++mPageSaveCounter;
		File htmlFile = new File(mLogDirPath, mFilePrefix + "_"
				+ mPageSaveCounter + ".html");
		try {
			FileOutputStream f = new FileOutputStream(htmlFile);
			f.write(source.getBytes());
			f.close();
			pageSaved = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		updateState(STATE.PAGE_SAVED, pageSaved);
	}

	@Override
	public void onJsPlaybackReady() {
		updateState(STATE.READY_FOR_VIDEO, true);
	}

	private void onVideoClicked() {
		int videoTimeoutMs = (int)(getTimeoutDuration() * 1000);
		logEvent("Activity", "onVideoClicked:{timeout:"
				+ videoTimeoutMs+"}");
		// start the timeout timer
		mVideoTimeoutHandler.postDelayed(mOnVideoTimeout, videoTimeoutMs);
	}
	
	@Override
	public void onJsPlaybackEnded(final String timeVal) {
		int endTime = (int)(Double.parseDouble(timeVal)*1000);
		int expectedEndTime = (int)(mCurrentVideo.getDuration()*1000);
		if (endTime >= expectedEndTime) {
			mVideoTimeoutHandler.removeCallbacks(mOnVideoTimeout);
			updateState(STATE.PLAYBACK_ENDED, true);
		}
	}

}