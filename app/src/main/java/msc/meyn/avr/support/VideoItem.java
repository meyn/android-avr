package msc.meyn.avr.support;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Locale;

public class VideoItem implements Parcelable {

	private final String LOGTAG = VideoItem.class.getSimpleName();

	public static final String KEY_SITE_ID = "siteId";

	private final String mId;
	private final String mUrl;
	private final VideoSite mSite;
	private final double mDuration;

	public VideoItem(String id, JSONObject videosEntryObj, VideoSite site)
			throws JSONException {
		this.mId = id;
		this.mSite = site;
		try {
			this.mUrl = URLDecoder.decode(videosEntryObj.getString("url"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new JSONException("invalid video url");
		}
		this.mDuration = videosEntryObj.getDouble("duration");
	}

	public VideoItem(Parcel source) {
		this.mId = source.readString();
		this.mSite = source.readParcelable(VideoSite.class.getClassLoader());
		this.mUrl = source.readString();
		this.mDuration = source.readDouble();
	}

	public String getId() {
		return mId;
	}

	public VideoSite getSite() {
		return mSite;
	}

	public String getUrl() {
		return mUrl;
	}

	public double getDuration() {
		return mDuration;
	}

	public boolean hasUrl() {
		return (mUrl != null && !mUrl.isEmpty());
	}

	public String createFilePrefix() {
		Calendar cal = Calendar.getInstance();
		cal.getTimeInMillis();
		return mId
				+ "_"
				+ String.format(Locale.US, "%ty%tm%td%tH%tM%tS", cal, cal, cal, cal, cal,
						cal);
	}

	@Override
	public String toString() {
		return LOGTAG + "@" + Integer.toHexString(hashCode()) + ":{" + mId
				+ "," + mUrl + "," + mSite + "," + mDuration + "}";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mId == null) ? 0 : mId.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		VideoItem other = (VideoItem) obj;
		if (mId == null) {
			if (other.mId != null) {
				return false;
			}
		} else if (!mId.equals(other.mId)) {
			return false;
		}
		return true;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeParcelable(mSite, flags);
		dest.writeString(mUrl);
		dest.writeDouble(mDuration);
	}

	public static final Parcelable.Creator<VideoItem> CREATOR = new Parcelable.Creator<VideoItem>() {
		@Override
		public VideoItem createFromParcel(Parcel source) {
			return new VideoItem(source);
		}

		@Override
		public VideoItem[] newArray(int size) {
			return new VideoItem[size];
		}
	};

}
