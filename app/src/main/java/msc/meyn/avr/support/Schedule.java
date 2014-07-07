package msc.meyn.avr.support;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public abstract class Schedule implements Parcelable {

	private final String type;
	private final int maxPlayCount;
	private final int intraListIntervalInSeconds;
	
	public Schedule(JSONObject scheduleObj) throws JSONException {
		this.type = scheduleObj.getString("type").trim();
		this.maxPlayCount = scheduleObj.getInt("autoRuns");
		this.intraListIntervalInSeconds = scheduleObj.getInt("intraListIntervalS");
	}
	
	public Schedule(Parcel source) {
		this.type = source.readString();
		this.maxPlayCount = source.readInt();
		this.intraListIntervalInSeconds = source.readInt();
	}

	public int getMaxPlayCount() {
		return maxPlayCount;
	}

	public int getIntraListInterval() {
		return intraListIntervalInSeconds;
	}
	
	public abstract Calendar getNextExecutionTime(int currentCount);
	
	public abstract boolean hasExpired();
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(type);
		dest.writeInt(maxPlayCount);
		dest.writeInt(intraListIntervalInSeconds);
	}

	public String toString() {
		return "Schedule:@"+ Integer.toHexString(hashCode())+ ":{" + type + "," + maxPlayCount + "," + intraListIntervalInSeconds + "}";
	}

}
