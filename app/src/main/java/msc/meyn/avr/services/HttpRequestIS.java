package msc.meyn.avr.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import msc.meyn.avr.Constants;

public class HttpRequestIS extends IntentService {

	private final static String CLASS_NAME = HttpRequestIS.class
			.getSimpleName();

	public static final String HTTP_METHOD_KEY = CLASS_NAME + ".HTTP_METHOD";
	public static final String HTTP_POST_PARAMS = CLASS_NAME
			+ ".HTTP_POST_PARAMS";
	public static final String HTTP_REQ_ID_KEY = CLASS_NAME + ".HTTP_REQ_ID";

	public enum Method {
		GET, POST
	};

	private final static HttpContext httpContext = new BasicHttpContext();
	private final static CookieStore cookieStore = new BasicCookieStore();
	private HttpClient httpClient;

	public HttpRequestIS() {
		super("MyIntentService");
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		httpClient = new DefaultHttpClient();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		httpClient.getConnectionManager().shutdown();
	}

	@Override
	protected void onHandleIntent(final Intent httpReqIntent) {
		Log.d(CLASS_NAME,
				"onHandleIntent(): " + httpReqIntent + ", "
						+ httpReqIntent.getDataString() + ", "
						+ httpReqIntent.getData());
		Bundle extras = httpReqIntent.getExtras();
		if (extras == null) {
			return;
		}

		Method httpMethod = (Method) extras.get(HTTP_METHOD_KEY);
		String reqId = extras.getString(HTTP_REQ_ID_KEY);
		String urlString = httpReqIntent.getDataString().trim();
		if (reqId == null || reqId.trim().isEmpty() || urlString == null
				|| urlString.trim().isEmpty()) {
			Log.e(CLASS_NAME, "onHandleIntent(): Invalid parameters.");
			return;
		}

		synchronized (this) {
			Intent localIntent = new Intent(reqId);
			try {
				String respString;
				HttpResponse response;
				URI serverUri = new URL(urlString).toURI();
				if (httpMethod == Method.GET) {
					response = httpClient.execute(new HttpGet(serverUri),
							httpContext);
				} else {
					@SuppressWarnings("unchecked")
					List<NameValuePair> postParams = (List<NameValuePair>) httpReqIntent
							.getExtras().get(HTTP_POST_PARAMS);
					HttpPost httppost = new HttpPost(serverUri);
					httppost.setEntity(new UrlEncodedFormEntity(postParams));
					response = httpClient.execute(httppost, httpContext);
				}
				respString = EntityUtils.toString(response.getEntity());
				localIntent.putExtra(Constants.EXTENDED_HTTP_RESPONSE_STATUS,
						response.getStatusLine().getStatusCode());
				localIntent.putExtra(Constants.EXTENDED_HTTP_RESPONSE_CONTENT,
						respString);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			LocalBroadcastManager.getInstance(HttpRequestIS.this)
					.sendBroadcast(localIntent);
			Log.d(CLASS_NAME, "broadcasted: " + localIntent);
		}
	}

}
