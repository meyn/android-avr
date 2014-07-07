package msc.meyn.avr.support;

import org.json.JSONException;
import org.json.JSONObject;

public class ScheduleFactory {
	
	public static Schedule getSchedule(JSONObject scheduleObj) throws JSONException {
		Schedule schedule = null;
		String type = scheduleObj.getString("type").trim();
		if (type.equals("tod_repeat")) {
			schedule = new ScheduleToDRepeat(scheduleObj);
		}
		return schedule;
	}
	
}
