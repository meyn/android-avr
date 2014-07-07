package msc.meyn.avr;

public final class Constants {
	private static final String CLASS_NAME = Constants.class.getName();

	public static final String ACTION_APP_STATE_CHANGE = CLASS_NAME
			+ ".ACTION_APP_STATE_CHANGE";

	public static final String ACTION_REQUEST_LIST = CLASS_NAME
			+ ".ACTION_REQUEST_LIST";
	public static final String ACTION_HTTP_RESPONSE = CLASS_NAME
			+ ".ACTION_HTTP_RESPONSE";

	public static final String EXTRA_VIDEO_ITEM = CLASS_NAME
			+ ".EXTRA_VIDEO_ITEM";

	public static final String EXTENDED_HTTP_RESPONSE_STATUS = CLASS_NAME
			+ ".HTTP_RESP_STATUS";
	public static final String EXTENDED_HTTP_RESPONSE_CONTENT = CLASS_NAME
			+ ".HTTP_RESP_CONTENT";

	public static final int SCHEDULED_APP_MAINTENANCE_INTENT_CODE = 1000;
	public static final int SCHEDULED_PLAY_PENDING_INTENT_CODE = 1001;
	public static final int SCHEDULED_LIST_DOWNLOAD_PENDING_INTENT = 1002;

	public static final String TCPDUMP_ASSET_NAME = "tcpdump";
	public static final String TCPDUMP_EXE_NAME = "meyn_tcpdump";
	public static final String LOCAL_LOG_DIR_NAME = "logs";
	public static final String REMOTE_LOG_DIR_NAME = "logs";

	public static final String VIDEO_API_REL_URL = "/videos/api/v1";
	public static final String ALL_LISTS_REL_URL = VIDEO_API_REL_URL + "/list/";
	public static final String CURRENT_LIST_REL_URL = VIDEO_API_REL_URL
			+ "/list/0";

	public static final int MAX_VIDEO_DURATION_SECS = 600; // 10 minutes
	public static final int DEFAULT_VIDEO_DURATION_SECS = 420; // 7 minutes

	public static final int PERIODIC_LOG_UPLOAD_ALARM = 0x1000;
}
