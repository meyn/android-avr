package msc.meyn.avr.browser;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class MeynVideoJsInterface {
	
	public static final String JS_API_NAME = "MeynMscDroidJsApi";
	public interface JsEvents {
		public void onJsLog(String msg);
		public void onJsSavePage(String source);
		public void onJsPlaybackReady();
		public void onJsPlaybackEnded(String timeVal);
	}
	
	private final JsEvents mListener;
	private static final String LOGTAG = MeynVideoJsInterface.class.getName();
	
	public MeynVideoJsInterface(final JsEvents listener) {
		if (listener == null) {
			throw new IllegalArgumentException("invalid Javascript event listener"); 
		}
		this.mListener = listener;
	}
	
	@JavascriptInterface
	public synchronized void log(final String msg) {
		Log.d(LOGTAG, "log(): " + msg);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mListener.onJsLog(msg);
			}
		});
	}
	
	@JavascriptInterface
	public synchronized void saveHTML2File(final String html) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mListener.onJsSavePage(html);
			}
		});
	}
	
	@JavascriptInterface
	public void dumpHTML(String html) {
		int length = html.length();
		for (int startIndex = 0; startIndex < length; startIndex += 2048) {
			if ((startIndex+2048) >= length) {
				Log.d("PageSource", html.substring(startIndex));
			} else {					
				Log.d("PageSource", html.substring(startIndex, startIndex+2048));
			}
		}
	}
	
	@JavascriptInterface
	public synchronized void readyToPlay() {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mListener.onJsPlaybackReady();
			}
		});
	}

	@JavascriptInterface
	public void onPlaybackEnded(final String timeVal) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mListener.onJsPlaybackEnded(timeVal);
			}
		});
	}

}
