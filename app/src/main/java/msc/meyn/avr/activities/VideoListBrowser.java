package msc.meyn.avr.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.Constants;
import msc.meyn.avr.R;
import msc.meyn.avr.services.HttpRequestIS;
import msc.meyn.avr.support.VideoList;

public class VideoListBrowser extends Activity implements OnClickListener,
		VideoListSelectDialog.Listener {

	private static final String CLASS_NAME = VideoListBrowser.class.getName();

	private static final String ACTION_DOWNLOAD_RESP = CLASS_NAME
			+ ".ACTION_DLD_RESP";
	private static SimpleDateFormat mSdf = new SimpleDateFormat(
			"MMM-dd HH:mm:ss", Locale.US);
	private static final int MAX_LISTS = 20;
	private static final int NUM_LISTS_PER_REQ = 5;
	private int mFirst, mLast;
	private JSONObject[] mDownloadedLists = new JSONObject[MAX_LISTS];
	private static ProgressDialog mProgressDialog;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setFinishOnTouchOutside(false);
		setContentView(R.layout.activity_list_browser);

		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setIndeterminate(true);

		findViewById(R.id.save_curr_msg).setOnClickListener(this);
		findViewById(R.id.fetch_list_btn).setOnClickListener(this);
		findViewById(R.id.del_current_list).setOnClickListener(this);

		resetListStartStop();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.save_curr_msg:
			VideoList list = ((AVRecorderApp) getApplication())
					.getCurrentList();
			if (list != null) {
				String msg = ((EditText) findViewById(R.id.curr_list_msg))
						.getText().toString().trim();
				list.setCaptureMsg(this, msg);
			}
			finish();
			break;
		case R.id.fetch_list_btn:
			resetListStartStop();
			getLists(mFirst, mLast);
			break;
		case R.id.del_current_list:
			updateCurrentListInfo(((AVRecorderApp) getApplication())
					.removeCurrentList());
			// getLists(mFirst, mLast);
			break;
		}
	}

	public void onResume() {
		super.onResume();
		updateCurrentListInfo(((AVRecorderApp) getApplication())
				.getCurrentList());
	}

	private void updateCurrentListInfo(VideoList list) {
		String title;
		Log.d(CLASS_NAME, "updateCurrentListInfo(): " + list);
		if (list != null) {
			title = "Current list:'" + list.getTitle() + "'\nTotal videos: "
					+ list.getNumVideos() + "\nAuto play's: "
					+ list.getPlayedCount() + " (of "
					+ list.getSchedule().getMaxPlayCount() + ")";
			Calendar nextRun = list.getNextExecutionTime();
			if (nextRun != null) {
				title += "\nNext run: " + mSdf.format(nextRun.getTime());
			} else {
				title += "\nPlease select another playlist.";
			}
			((EditText) findViewById(R.id.curr_list_msg)).setText(list
					.getCaptureMsg());
			findViewById(R.id.del_current_list).setVisibility(View.VISIBLE);
			findViewById(R.id.curr_list_msg_layout).setVisibility(
					LinearLayout.VISIBLE);
		} else {
			title = "Please select a playlist.";
			findViewById(R.id.curr_list_msg_layout).setVisibility(
					LinearLayout.GONE);
			findViewById(R.id.del_current_list).setVisibility(View.GONE);
		}
		((TextView) findViewById(R.id.curr_list_info)).setText(title);
	}

	private void resetListStartStop() {
		mFirst = 0;
		mLast = NUM_LISTS_PER_REQ - 1;
	}

	private void selectList(int index) {
		Log.d(CLASS_NAME, "selectList(): " + index + ": " + ", "
				+ (mFirst + index) + mDownloadedLists[mFirst + index]);
		try {
			AVRecorderApp app = (AVRecorderApp) getApplication();
			VideoList availableList = app.getCurrentList();
			VideoList list = new VideoList(mDownloadedLists[mFirst + index],
					this);
			if (availableList != null) {
				if (availableList.getId().equals(list.getId())) {
					return;
				}
			}
			updateCurrentListInfo(app.setCurrentList(list,
					mDownloadedLists[mFirst + index].toString()));
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e(CLASS_NAME, e.getMessage());
		}
	}

	private void getLists(int first, int last) {
		String serverUrl = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("pref_key_server_url_value",
				getResources().getString(R.string.pref_server_url_default));
		Intent getListIntent = new Intent(getApplicationContext(),
				HttpRequestIS.class);
		getListIntent
				.setData(
						Uri.parse(serverUrl + Constants.ALL_LISTS_REL_URL
								+ "?from=" + first + "&to=" + last))
				.putExtra(HttpRequestIS.HTTP_METHOD_KEY,
						HttpRequestIS.Method.GET)
				.putExtra(HttpRequestIS.HTTP_REQ_ID_KEY, ACTION_DOWNLOAD_RESP);

		LocalBroadcastManager.getInstance(this).registerReceiver(
				mBroadcastReceiver, new IntentFilter(ACTION_DOWNLOAD_RESP));
		startService(getListIntent);
		if (!mProgressDialog.isShowing()) {
			mProgressDialog.setMessage("Getting list(s)");
			mProgressDialog.show();
		}
	}

	private void handleGetResponse(String resp) {
		Log.d(CLASS_NAME, "handleGetResponse(): " + resp);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mBroadcastReceiver);
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}

		try {
			if (resp != null && !resp.trim().isEmpty()) {
				JSONArray lists = new JSONArray(resp);
				CharSequence[] titles = new CharSequence[lists.length()];
				for (int i = 0; i < lists.length(); ++i) {
					mDownloadedLists[mFirst + i] = new JSONObject(
							lists.getString(i));
					titles[i] = mDownloadedLists[mFirst + i].getString("title");
					Log.d(CLASS_NAME, "RX [" + i + "]: "
							+ mDownloadedLists[mFirst + i]);
				}
				if (titles.length > 0) {
					if (titles.length == 1) {
						selectList(0);
					} else {
						VideoListSelectDialog.newInstance(titles).show(
								getFragmentManager(), "PlaylistSelectDialog");
					}
					return;
				}
			}
			resetListStartStop();
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e(CLASS_NAME, e.getMessage());
		}
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(CLASS_NAME, "onReceive(): " + context + ", " + intent + ", "
					+ intent.getAction());
			if (intent.getAction().equals(ACTION_DOWNLOAD_RESP)) {
				Bundle extras = intent.getExtras();
				Log.d(CLASS_NAME, intent.getAction() + ": " + extras);
				if ((extras != null)
						&& (extras
								.getInt(Constants.EXTENDED_HTTP_RESPONSE_STATUS) == HttpStatus.SC_OK)) {
					handleGetResponse(extras.getString(
							Constants.EXTENDED_HTTP_RESPONSE_CONTENT, null));
				} else {
					handleGetResponse(null);
				}
			}
		}
	};

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		mFirst = mLast + 1;
		mLast += NUM_LISTS_PER_REQ;
		getLists(mFirst, mLast);
	}

	@Override
	public void onDialogListChosen(DialogFragment dialog, int index) {
		selectList(index);
	}

}
