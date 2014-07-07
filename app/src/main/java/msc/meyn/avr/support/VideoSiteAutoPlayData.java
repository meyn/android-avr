package msc.meyn.avr.support;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

public class VideoSiteAutoPlayData {

	private final static String LOGTAG = "AutoPlayEventData";

	private final String key;
	private int scrollX, scrollY;
	private int numEvents;
	private ArrayList<AutoPlayMotionEvent> events;

	public VideoSiteAutoPlayData(int orientation) {
		key = orientation + "_";
		numEvents = 0;
		events = new ArrayList<AutoPlayMotionEvent>();
	}

	public void writeScrollData(int scrollX, int scrollY,
			SharedPreferences prefs) {
		this.scrollX = scrollX;
		this.scrollY = scrollY;
		prefs.edit().putInt(key + "scrollX", scrollX)
				.putInt(key + "scrollY", scrollY).apply();
		Log.w(LOGTAG, "writeScrollData(): " + this);
	}

	public void writeActionData(ArrayList<MotionEvent> events,
			SharedPreferences prefs) {
		int count = 0;
		Editor edit = prefs.edit();
		for (MotionEvent event : events) {
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_UP) {
				String countKey = key + count++ + "_";
				edit.putInt(countKey + "act", event.getAction())
						.putInt(countKey + "x", (int) event.getX())
						.putInt(countKey + "y", (int) event.getY())
						.putInt(countKey + "dId", (int) event.getDeviceId())
						.putInt(countKey + "src", (int) event.getSource()).apply();
			}
		}
		prefs.edit().putInt(key + "numEvents", count).apply();
	}

	public VideoSiteAutoPlayData read(SharedPreferences prefs) {
		this.scrollX = prefs.getInt(key + "scrollX", -1);
		this.scrollY = prefs.getInt(key + "scrollY", -1);
		this.numEvents = prefs.getInt(key + "numEvents", 0);
		events = new ArrayList<AutoPlayMotionEvent>(numEvents);
		for (int i = 0; i < numEvents; ++i) {
			String countKey = key + i + "_";
			events.add(new AutoPlayMotionEvent(prefs.getInt(countKey + "act",
					-1), prefs.getInt(countKey + "x", -1), prefs.getInt(
					countKey + "y", -1), prefs.getInt(countKey + "dId", 1),
					prefs.getInt(countKey + "src", 0)));
		}
		return this;
	}

	public int setValue(String type, int value) {
		int ret = -1;
		if (key.equals("scrollX")) {
			this.scrollX = ret = value;
		} else if (key.equals("scrollY")) {
			this.scrollY = ret = value;
		}
		return ret;
	}

	public int getNumEvents() {
		return this.numEvents;
	}

	public ArrayList<AutoPlayMotionEvent> getEvents() {
		return this.events;
	}

	public int getScrollX() {
		return this.scrollX;
	}

	public int getScrollY() {
		return this.scrollY;
	}

	public String toString() {
		return "{" + key + "," + scrollX + "," + scrollY + ',' + events + "}";
	}

	public class AutoPlayMotionEvent {
		public final int action;
		public final float x, y;
		public final int source;
		public final int deviceId;

		public AutoPlayMotionEvent(int action, float x, float y, int source,
				int deviceId) {
			this.action = action;
			this.x = x;
			this.y = y;
			this.source = source;
			this.deviceId = deviceId;
		}

		public String toString() {
			return "{" + action + "," + x + "," + y + "," + source + ","
					+ deviceId + "}";
		}
	}

}
