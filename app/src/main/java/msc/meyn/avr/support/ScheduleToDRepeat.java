package msc.meyn.avr.support;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class ScheduleToDRepeat extends Schedule {
	
	private final int number;
	private final int intervalInMinutes;
	private final String startTime;
	private final Calendar calStartTime;
	
	private final String LOGTAG = "ScheduleToDRepeat";
	
	public ScheduleToDRepeat(JSONObject scheduleObj) throws JSONException {
		super(scheduleObj);
		this.number = scheduleObj.getInt("autoRuns");
		this.intervalInMinutes = scheduleObj.getInt("intervalM");
		this.startTime = scheduleObj.getString("start");
		calStartTime = getCalStartTime(this.startTime);
	}
	
	public ScheduleToDRepeat(Parcel source) {
		super(source);
		this.number = source.readInt();
		this.intervalInMinutes = source.readInt();
		this.startTime = source.readString();
		this.calStartTime = getCalStartTime(this.startTime);
	}
	
	@Override
	public Calendar getNextExecutionTime(int currentCount) {
		if ((currentCount < 0) || currentCount >= number) {
			return null;
		}
		
		Calendar now = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd HH:mm:ss");
		Log.d(LOGTAG, "startTime="+startTime+", now="+sdf.format(now.getTime()) +", "+ now);
		
		if (calStartTime.after(now)) {
			return calStartTime;
		}
		else {
			Calendar next = calStartTime;
			while(next.before(now)) {
				next.add(Calendar.MINUTE, intervalInMinutes);
			}
			return next;
		}
	}
	
	@Override
	public boolean hasExpired() {
		return false;
	}
	
	@Override
	public String toString() {
		return super.toString() + "@" + Integer.toHexString(hashCode()) + ":{" + number + "," + intervalInMinutes + "," + startTime+ "}";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(number);
		dest.writeInt(intervalInMinutes);
		dest.writeString(startTime);
	}
	
	private Calendar getCalStartTime(String startTimeStr) {
		Calendar cal = Calendar.getInstance();
		String dateParts[] = startTimeStr.split(":");
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(dateParts[0]));
		cal.set(Calendar.MINUTE, Integer.parseInt(dateParts[1]));
		cal.set(Calendar.SECOND, Integer.parseInt(dateParts[2]));
		return cal;
	}
	
	public static final Parcelable.Creator<ScheduleToDRepeat> CREATOR = new Parcelable.Creator<ScheduleToDRepeat>() {
		@Override
		public ScheduleToDRepeat createFromParcel(Parcel source) {
			return new ScheduleToDRepeat(source);
		}

		@Override
		public ScheduleToDRepeat[] newArray(int size) {
			return new ScheduleToDRepeat[size];
		}
	};

}
