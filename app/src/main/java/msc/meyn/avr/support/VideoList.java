package msc.meyn.avr.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import msc.meyn.avr.Storage;

public class VideoList implements Parcelable {

	private final static String CLASS_NAME = VideoList.class.getSimpleName();

	private final static String KEY_SCHEDULE = "schedule";
	private final static String KEY_VIDEOS = "videos";

	private final String mId;
	private final String mTitle;
	private final int mVersion;
	private final Schedule mSchedule;
	private final Map<String, VideoSite> mSitesMap;
	private final HashSet<VideoItem> mVideoItems;
	private int mPlayedCount;
	private String mCaptureMsg;

	/**
	 * 
	 * @param listObj
	 * @param context
	 * @throws JSONException
	 */
	public VideoList(JSONObject listObj, Context context) throws JSONException {
		JSONObject listEntryVideos = listObj.getJSONObject(KEY_VIDEOS);
		Log.d(CLASS_NAME,
				"VideoList(): " + listObj + ", " + listEntryVideos.length()
						+ ", " + listEntryVideos);

		this.mVideoItems = new HashSet<VideoItem>();
		this.mSitesMap = new HashMap<String, VideoSite>();

		this.mId = listObj.getString("id");
		this.mTitle = listObj.getString("title");
		this.mVersion = listObj.getInt("version");
		this.mSchedule = ScheduleFactory.getSchedule(listObj
				.getJSONObject(KEY_SCHEDULE));

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		this.mPlayedCount = prefs.getInt(Storage.KEY_LIST_PLAYED_COUNTER, 0);
		this.mCaptureMsg = Storage.readCaptureMsg(prefs);

		for (Iterator<?> iter = listEntryVideos.keys(); iter.hasNext();) {
			String key = (String) iter.next();
			if (listEntryVideos.get(key) instanceof JSONObject) {
				JSONObject video = listEntryVideos.getJSONObject(key);
				String siteId = video.getString(VideoItem.KEY_SITE_ID);
				VideoSite site = this.mSitesMap.get(siteId);
				if (site == null) {
					try {
						site = new VideoSite(siteId, context);
						this.mSitesMap.put(siteId, site);
					} catch (InstantiationException e) {
						e.printStackTrace();
						Log.e(CLASS_NAME, e.getMessage());
						continue;
					}
				}
				this.mVideoItems.add(new VideoItem(key, video, this.mSitesMap
						.get(video.getString(VideoItem.KEY_SITE_ID))));
			}
		}
	}

	/**
	 * 
	 * @param source
	 */
	public VideoList(Parcel source) {
		this.mSitesMap = new HashMap<String, VideoSite>();
		this.mVideoItems = new HashSet<VideoItem>();

		this.mId = source.readString();
		this.mTitle = source.readString();
		this.mVersion = source.readInt();
		this.mSchedule = source.readParcelable(Schedule.class.getClassLoader());
		Parcelable[] videos = source.readParcelableArray(VideoItem.class
				.getClassLoader());
		for (int index = 0; index < videos.length; ++index) {
			VideoItem video = (VideoItem) videos[index];
			this.mVideoItems.add(video);
			VideoSite site = video.getSite();
			if (site != null) {
				this.mSitesMap.put(site.getId(), site);
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getId() {
		return mId;
	}

	/**
	 * 
	 * @return
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * 
	 * @return
	 */
	public int getNumVideos() {
		return mVideoItems.size();
	}
	
	/**
	 * 
	 * @param context
	 * @param strRep
	 */
	public VideoList storeList(Context context, String strRep) {
		if (strRep != null) {
			Storage.writeVideoList(
					PreferenceManager.getDefaultSharedPreferences(context),
					strRep);
			return this;
		} else {
			removeList(context);
			return null;
		}
	}

	/**
	 * 
	 * @param context
	 */
	public VideoList removeList(Context context) {
		Storage.unwriteVideoList(PreferenceManager
				.getDefaultSharedPreferences(context));
		return null;
	}

	/**
	 * 
	 * @param context
	 * @param msg
	 */
	public void setCaptureMsg(Context context, String msg) {
		Log.d(CLASS_NAME, "setCaptureMsg(): " + msg);
		mCaptureMsg = msg;
		Storage.writeCaptureMsg(
				PreferenceManager.getDefaultSharedPreferences(context),
				mCaptureMsg);
	}

	/**
	 * 
	 * @return
	 */
	public String getCaptureMsg() {
		return mCaptureMsg;
	}

	/**
	 * 
	 * @param context
	 */
	public void incAutoPlayCount(Context context) {
		++mPlayedCount;
		Storage.writePlayedCount(
				PreferenceManager.getDefaultSharedPreferences(context),
				mPlayedCount);
	}

	/**
	 * 
	 * @return
	 */
	public int getPlayedCount() {
		return mPlayedCount;
	}

	/**
	 * 
	 */
	public Calendar getNextExecutionTime() {
		return mSchedule.getNextExecutionTime(getPlayedCount());
	}

	/**
	 * 
	 * @return
	 */
	public Schedule getSchedule() {
		return mSchedule;
	}

	/**
	 * 
	 */
	public boolean hasExpired() {
		return ((mPlayedCount >= mSchedule.getMaxPlayCount()) || (mSchedule
				.hasExpired()));
	}

	/**
	 * 
	 * @param context
	 * @return
	 */
	private HashSet<VideoSite> getUntrainedSites(Context context) {
		HashSet<VideoSite> untrainedSites = new HashSet<VideoSite>();
		Set<String> trainedSites = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getStringSet(VideoSite.TRAINED_SITES_PREF_KEY,
						new HashSet<String>());
		for (Map.Entry<String, VideoSite> entry : mSitesMap.entrySet()) {
			if (!trainedSites.contains(entry.getValue().getId())) {
				untrainedSites.add(entry.getValue());
			}
		}
		Log.d(CLASS_NAME, "getUntrainedSites(): " + untrainedSites);
		return untrainedSites;
	}

	/**
	 * 
	 * @param context
	 * @return
	 */
	public ArrayList<VideoItem> getUntrainedVideoItems(Context context) {
		HashSet<VideoSite> addedSites = new HashSet<VideoSite>();
		HashSet<VideoSite> untrainedSites = getUntrainedSites(context);
		HashSet<VideoItem> videos = new HashSet<VideoItem>();
		for (VideoItem videoItem : mVideoItems) {
			VideoSite currentSite = videoItem.getSite();
			if (!addedSites.contains(currentSite)) { // only 1 per site
				addedSites.add(currentSite);
				if (untrainedSites.contains(currentSite)) {
					videos.add(videoItem);
				}
			}
		}
		Log.d(CLASS_NAME, "getUntrainedVideoItems(): " + videos);
		return new ArrayList<VideoItem>(videos);
	}

	/**
	 * 
	 * @param context
	 * @return
	 */
	public HashSet<VideoSite> getTrainedSites(Context context) {
		HashSet<VideoSite> sites = new HashSet<VideoSite>();
		Set<String> trainedSites = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getStringSet(VideoSite.TRAINED_SITES_PREF_KEY,
						new HashSet<String>());
		for (Map.Entry<String, VideoSite> entry : mSitesMap.entrySet()) {
			if (trainedSites.contains(entry.getValue().getId())) {
				sites.add(entry.getValue());
			}
		}
		Log.d(CLASS_NAME, "getTrainedSites(): " + sites);
		return sites;
	}

	/**
	 * 
	 * @param context
	 * @return
	 */
	public ArrayList<VideoItem> getTrainedVideoItems(Context context) {
		HashSet<VideoItem> videos = new HashSet<VideoItem>();
		HashSet<VideoSite> trainedSites = getTrainedSites(context);
		for (VideoItem videoItem : mVideoItems) {
			if (trainedSites.contains(videoItem.getSite())) {
				videos.add(videoItem);
			}
		}
		Log.d(CLASS_NAME, "getTrainedVideos(): " + videos);
		return new ArrayList<VideoItem>(videos);
	}

	/**
	 * 
	 * @param context
	 * @return
	 */
	public ArrayList<VideoItem> getTrainedVideoItemsSample(Context context) {
		HashSet<VideoSite> addedSites = new HashSet<VideoSite>();
		HashSet<VideoSite> trainedSites = getTrainedSites(context);
		HashSet<VideoItem> videos = new HashSet<VideoItem>();
		for (VideoItem videoItem : mVideoItems) {
			VideoSite currentSite = videoItem.getSite();
			if (!addedSites.contains(currentSite)) { // only 1 per site
				addedSites.add(currentSite);
				if (trainedSites.contains(currentSite)) {
					videos.add(videoItem);
				}
			}
		}
		Log.d(CLASS_NAME, "getTrainedVideoItemsSample(): " + videos);
		return new ArrayList<VideoItem>(videos);
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<VideoItem> getAllVideos() {
		return new ArrayList<VideoItem>(mVideoItems);
	}

	public Collection<VideoSite> getAllVideoSites() {
		Log.d(CLASS_NAME, "keys = " + mSitesMap.keySet() + ", values = "
				+ mSitesMap.values());
		return mSitesMap.values();
	}

	/**
	 * 
	 */
	public String toString() {
		return CLASS_NAME + "@" + Integer.toHexString(hashCode()) + ":{" + mId
				+ "," + mTitle + "," + mVersion + "," + mSchedule + ","
				+ mVideoItems + "," + mSitesMap + "," + mPlayedCount + ","
				+ mCaptureMsg + "}";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeString(mTitle);
		dest.writeInt(mVersion);
		dest.writeParcelable(mSchedule, flags);
		int index = 0;
		VideoItem videos[] = new VideoItem[mVideoItems.size()];
		for (VideoItem video : mVideoItems) {
			videos[index++] = video;
		}
		dest.writeParcelableArray(videos, flags);
	}

	public static final Parcelable.Creator<VideoList> CREATOR = new Parcelable.Creator<VideoList>() {
		@Override
		public VideoList createFromParcel(Parcel source) {
			return new VideoList(source);
		}

		@Override
		public VideoList[] newArray(int size) {
			return new VideoList[size];
		}
	};

}
