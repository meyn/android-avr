package msc.meyn.avr.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

public class UploadFileFTPIS extends IntentService {
	
	private final static String CLASS_NAME = UploadFileFTPIS.class.getName();
	
	public final static String EXTRA_KEY_RESP_ACTION_INTENT = CLASS_NAME + ".RESP_INTENT";
	public final static String EXTRA_KEY_LOCAL_BASE_DIR = CLASS_NAME+".LOCAL_BASE_DIR";
	public final static String EXTRA_KEY_REMOTE_LOG_DIR = CLASS_NAME+".REMOTE_LOG_DIR";
	public final static String EXTRA_KEY_FILES_LIST = CLASS_NAME + ".FILES_LIST";
	
	public final static String EXTRA_KEY_COMPLETION_SUCCESS = CLASS_NAME + ".COMPLETION_STATUS";
	
	public UploadFileFTPIS() {
		super(CLASS_NAME);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(CLASS_NAME, "onHandleIntent(): " + intent);
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}
		String respAction = extras.getString(EXTRA_KEY_RESP_ACTION_INTENT);
		if (respAction == null) {
			return;
		}
		Intent localIntent = new Intent(respAction);
		localIntent.putExtra(EXTRA_KEY_COMPLETION_SUCCESS, false);
		String baseDir = extras.getString(EXTRA_KEY_LOCAL_BASE_DIR, null);
		String serverDir = extras.getString(EXTRA_KEY_REMOTE_LOG_DIR, null);
		ArrayList<String> files = extras.getStringArrayList(EXTRA_KEY_FILES_LIST);
		if (baseDir == null || serverDir == null || files.size() < 1) {
			return;
		}
		String deviceId = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		FTPClient ftpClient = new FTPClient();
		
		try {
			ftpClient.connect("mvcrawl.cs.hut.fi", 2014);
			Log.d(CLASS_NAME, Arrays.toString(ftpClient.getReplyStrings()));
			if (ftpClient.login("<USERNAME>", "<PASSWD>")) {
				if (ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
					ftpClient.enterLocalPassiveMode();
					for (int i = 0; i < files.size(); ++i) {
						String fileName = files.get(i);
						InputStream inStream = new FileInputStream(baseDir + "/"
								+ fileName);
						Log.d(CLASS_NAME, "uploading: " + fileName);
						if (ftpClient.storeFile(serverDir + "/" + deviceId + "_" + fileName, inStream)) {
							// delete after upload
							File file = new File(baseDir + "/" + fileName);
							file.delete();
						}
						Log.d(CLASS_NAME, "storeFile(): " + Arrays.toString(ftpClient.getReplyStrings()));
						try {
							inStream.close();
						} catch (IOException e) {
							Log.e(CLASS_NAME, e.getMessage());
						}
					}
					ftpClient.disconnect();
					localIntent.putExtra(EXTRA_KEY_COMPLETION_SUCCESS, true);
					Log.d(CLASS_NAME, Arrays.toString(ftpClient.getReplyStrings()));
				} else {
					ftpClient.disconnect();
					Log.d(CLASS_NAME, Arrays.toString(ftpClient.getReplyStrings()));
				}
			} else {
				ftpClient.disconnect();
				Log.d(CLASS_NAME, Arrays.toString(ftpClient.getReplyStrings()));
			}
		} catch (SocketException e) {
			Log.e(CLASS_NAME, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(CLASS_NAME, e.getMessage());
			e.printStackTrace();
		}
		LocalBroadcastManager.getInstance(UploadFileFTPIS.this)
		.sendBroadcast(localIntent);
		Log.d(CLASS_NAME, "broadcasted: " + localIntent.getAction());
	}

}
