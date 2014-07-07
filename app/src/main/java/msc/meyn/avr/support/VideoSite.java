package msc.meyn.avr.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import msc.meyn.avr.R;

public final class VideoSite implements Parcelable {

	public static final String TRAINED_SITES_PREF_KEY = "trained_sites";
	public static final String EXTRA_VIDEO_ITEM = "extra_video_item";

	private static final String LOGTAG = VideoSite.class.getSimpleName();

	private final String mId;
	private String mTitle;

	public VideoSite(String id, Context context) throws InstantiationException {
		this.mId = id;
		String[] siteIdTitleMap = context.getResources().getStringArray(
				R.array.video_site_titles);
		for (String entry : siteIdTitleMap) {
			String[] parts = entry.split(",");
			if (parts[0].equals(id)) {
				this.mTitle = parts[1];
				break;
			}
		}
		if (this.mTitle == null) {
			throw new InstantiationException("Unknown site: " + id);
		}
	}

	public VideoSite(Parcel source) {
		this.mId = source.readString();
		this.mTitle = source.readString();
	}

	public void writeAutoPlayData(int orientation, int scrollX, int scrollY,
			ArrayList<MotionEvent> events, Context context) {
		SharedPreferences defaultPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences prefs = context.getSharedPreferences(mId
				+ "_sim_data", Context.MODE_PRIVATE);
		VideoSiteAutoPlayData autoPlayEvent = new VideoSiteAutoPlayData(
				orientation);
		if (events.size() > 0) {
			autoPlayEvent.writeScrollData(scrollX, scrollY, prefs);
			autoPlayEvent.writeActionData(events, prefs);
			Set<String> trainedSiteIds = defaultPrefs.getStringSet(
					TRAINED_SITES_PREF_KEY, new HashSet<String>());
			trainedSiteIds.add(mId);
			defaultPrefs.edit()
					.putStringSet(TRAINED_SITES_PREF_KEY, trainedSiteIds)
					.apply();
		}
	}

	public VideoSiteAutoPlayData getAutoPlayEventData(int orientation,
			Context context) {
		Log.d(LOGTAG, "getAutoPlayEventData(): " + orientation);
		VideoSiteAutoPlayData autoPlayData = new VideoSiteAutoPlayData(
				orientation);
		SharedPreferences prefs = context.getSharedPreferences(mId
				+ "_sim_data", Context.MODE_PRIVATE);
		return autoPlayData.read(prefs);
	}

	public void unwriteAutoPlayData(Context context) {
		Log.d(LOGTAG, "unwriteAutoPlayData()");
		SharedPreferences defaultPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences prefs = context.getSharedPreferences(mId
				+ "_sim_data", Context.MODE_PRIVATE);
		prefs.edit().clear().apply();
		Set<String> trainedSiteIds = defaultPrefs.getStringSet(
				TRAINED_SITES_PREF_KEY, new HashSet<String>());
		trainedSiteIds.remove(mId);
		defaultPrefs.edit()
				.putStringSet(TRAINED_SITES_PREF_KEY, trainedSiteIds)
				.apply();
	}

	public String getId() {
		return mId;
	}

	public String getTitle() {
		return mTitle;
	}

	public boolean isAutoPlayable(int orientation, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(mId
				+ "_sim_data", Context.MODE_PRIVATE);
		Map<String, ?> contents = prefs.getAll();
		Log.d(LOGTAG, "isAutoPlayable(): " + contents.size() + ":" + contents);
		return (contents.size() > 0 ? true : false);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mId == null) ? 0 : mId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VideoSite other = (VideoSite) obj;
		if (mId == null) {
			if (other.mId != null)
				return false;
		} else if (!mId.equals(other.mId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return LOGTAG + "@" + Integer.toHexString(hashCode()) + ":{" + mId
				+ "," + mTitle + "}";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeString(mTitle);
	}

	public static final Parcelable.Creator<VideoSite> CREATOR = new Parcelable.Creator<VideoSite>() {
		@Override
		public VideoSite createFromParcel(Parcel source) {
			return new VideoSite(source);
		}

		@Override
		public VideoSite[] newArray(int size) {
			return new VideoSite[size];
		}
	};

}
