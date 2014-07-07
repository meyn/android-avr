package msc.meyn.avr.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class MeynWebView extends WebView {

	//TODO: Mainly used in debug - needs to be cleaned up / removed
	public MeynWebView(Context context) {
		super(context);
		init();
	}

	public MeynWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MeynWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void init() {
//		this.getSettings().setPluginState(PluginState.OFF);
		this.getSettings().setJavaScriptEnabled(true);
	}

	@Override
	public void addJavascriptInterface(Object obj, String interfaceName) {
		if (obj instanceof MeynVideoJsInterface) {
			super.addJavascriptInterface(obj, interfaceName);
		}
	}

}
