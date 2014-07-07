package msc.meyn.avr;

import android.content.SharedPreferences;

public class Storage {

	public final static String KEY_LIST_OBJ_STRING = "video_list_obj";
	public final static String KEY_LIST_PLAYED_COUNTER = "video_list_played_count";
	public final static String KEY_LIST_TRAINED_SITES = "trained_sites";
	public final static String KEY_LIST_CAPTURE_MSG = "video_list_msg";

	public static void writeVideoList(SharedPreferences prefs,
			String videoListStr) {
		prefs.edit().putInt(KEY_LIST_PLAYED_COUNTER, 0)
				.putString(KEY_LIST_OBJ_STRING, videoListStr).apply();
	}

	public static String readVideoList(SharedPreferences prefs) {
		return prefs.getString(KEY_LIST_OBJ_STRING, null);
	}

	public static void unwriteVideoList(SharedPreferences prefs) {
		prefs.edit().remove(KEY_LIST_PLAYED_COUNTER)
				.remove(KEY_LIST_OBJ_STRING).apply();
	}

	public static void writePlayedCount(SharedPreferences prefs, int count) {
		prefs.edit().putInt(KEY_LIST_PLAYED_COUNTER, count).apply();
	}

	public static void unwritePlayedCount(SharedPreferences prefs) {
		prefs.edit().remove(KEY_LIST_PLAYED_COUNTER).apply();
	}

	public static void writeCaptureMsg(SharedPreferences prefs, String msg) {
		prefs.edit().putString(KEY_LIST_CAPTURE_MSG, msg).apply();
	}
	
	public static String readCaptureMsg(SharedPreferences prefs) {
		return prefs.getString(KEY_LIST_CAPTURE_MSG, "");
	}

	public static void unwriteCaptureMsg(SharedPreferences prefs) {
		prefs.edit().remove(KEY_LIST_CAPTURE_MSG).apply();
	}

}
