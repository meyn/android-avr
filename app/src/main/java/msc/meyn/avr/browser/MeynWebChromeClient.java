package msc.meyn.avr.browser;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import msc.meyn.avr.activities.Recorder;

public class MeynWebChromeClient extends WebChromeClient  implements OnPreparedListener, OnCompletionListener {
	
	public interface WCCEvents {
		public void onConsoleMsg(String msg);
	}
	
	private static final String LOGTAG = "Meyn_WCC";
	private final Recorder activity;
	private CustomViewCallback mCustomViewCallback;
	
	// TODO: Mainly debug code here - needs to be cleaned up
	public MeynWebChromeClient(final Recorder activity) {
		super();
		this.activity = activity;
	}
		
	@Override
	public void onProgressChanged(WebView view, int newProgress) {
//		super.onProgressChanged(view, newProgress);
		Log.d(LOGTAG, "onProgressChanged(): " + newProgress);
	}

	@Override
	public void onReceivedTitle(WebView view, String title) {
//		super.onReceivedTitle(view, title);
		Log.d(LOGTAG, "onReceivedTitle");
	}
	
	@Override
	public void onReceivedIcon(WebView view, Bitmap icon) {
//		super.onReceivedIcon(view, icon);
		Log.d(LOGTAG, "onReceivedIcon");
	}
	
	@Override
	public void onShowCustomView(View view, CustomViewCallback callback) {
		Log.d(LOGTAG, "onShowCustomView(): " + view);
		super.onShowCustomView(view, callback);
		mCustomViewCallback = callback;
	}

	@Override
	public void onHideCustomView() {
//		super.onHideCustomView();
		Log.d(LOGTAG, "onHideCustomView");
		if (mCustomViewCallback != null) {
			mCustomViewCallback.onCustomViewHidden();
		}
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				if (activity != null) {
					activity.onJsPlaybackEnded("-1");
				}
			}
		});
	}
	
	public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
//		super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
		Log.d(LOGTAG, "onCreateWindow");
		return false;
	}
	
	public void onRequestFocus(WebView view) {
//		super.onRequestFocus(view);
		Log.d(LOGTAG, "onRequestFocus");
	}
	
	public boolean onConsoleMessage(ConsoleMessage cm) {
		return false;
	}
	
	public void onCloseWindow(WebView window) {
//		super.onCloseWindow(window);
		Log.d(LOGTAG, "onCloseWindow");
	}
	public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
		Log.d(LOGTAG, "onJsAlert");
		return false;
	}
	public boolean onJsConfirm(WebView view, String url, String message,  JsResult result) {
		Log.d(LOGTAG, "onJsConfirm");
		return false;
	}
	public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
		Log.d(LOGTAG, "onJsPrompt");
		return false;
	}
	
	public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
		Log.d(LOGTAG, "onJsBeforeUnload");
        return false;
	}
	public boolean onJsTimeout() {
		Log.d(LOGTAG, "onJsTimeout");
		return true;
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		Log.d(LOGTAG, "onPrepared(): ");
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d(LOGTAG, "onCompletion()");
	}
}
