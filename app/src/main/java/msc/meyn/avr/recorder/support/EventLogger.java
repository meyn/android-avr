package msc.meyn.avr.recorder.support;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import msc.meyn.avr.support.VideoList;

public class EventLogger implements Runnable {

	public interface LoggerEvents {
		public void onLoggingStarted();

		public void onLoggingStopped(boolean status);
	}

	private final LoggerEvents mListener;
	private final BufferedWriter mWriter;
	private volatile Thread mCurrentThread;
	private final BlockingQueue<String> mMsgQueue = new ArrayBlockingQueue<String>(
			100);

	private final Context mContext;
	private int mSysInfoSaveCounter = 0;

	private static final int LOGFILE_VERSION = 1;

	public EventLogger(LoggerEvents listener, Context context, File logFile)
			throws IOException {
		this.mListener = listener;
		this.mContext = context;
		logFile.createNewFile();
		mWriter = new BufferedWriter(new FileWriter(logFile));
	}

	/**
	 * 
	 */
	public void start() {
		if (mCurrentThread == null) {
			mCurrentThread = new Thread(this);
			mCurrentThread.start();
			writeLine("log_ver: " + LOGFILE_VERSION);
			writePackageInfo();
			writeDeviceInfo();
			writeNetworkInfo();
		}
	}

	public void logEvent(String src, String msg) {
		writeLine("\"event\":{\"avrTime\":"
				+ (System.currentTimeMillis() / 1000L) + ",\"src\":\"" + src
				+ "\",\"msg\":\"" + msg + "\"}");
	}

	private void writeLine(String msg) {
//		Log.i("LOGGER", "writeLine: " + msg);
		mMsgQueue.add(msg + '\n');
	}

	/**
	 * 
	 */
	private void writePackageInfo() {
		try {
			PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(
					mContext.getPackageName(), 0);
			writeLine("\"pkg\":{\"ver\":" + pInfo.versionCode
					+ ", \"name\":\"" + pInfo.versionName + "\"}");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			writeLine("\"pkg\":{\"error\":" + e.getMessage() + "}");
		}
	}

	/**
	 * 
	 */
	private void writeDeviceInfo() {
		writeLine("\"device\":{\"build\":[" + "\""
				+ android.os.Build.VERSION.CODENAME + "\",\""
				+ android.os.Build.VERSION.INCREMENTAL + "\",\""
				+ android.os.Build.VERSION.RELEASE + "\",\""
				+ android.os.Build.MANUFACTURER + "\",\""
				+ android.os.Build.MODEL + "\",\"" + android.os.Build.BRAND
				+ "\",\"" + android.os.Build.DEVICE + "\",\""
				+ android.os.Build.PRODUCT + "\"], \"arch\":\""
				+ System.getProperty("os.arch") + "\"}");
	}

	/**
	 * 
	 */
	private void writeNetworkInfo() {
		String msg = "\"network\":{";
		TelephonyManager telManager = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		NetworkInfo activeNetwork = ((ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		msg += "\"name\":\"" + telManager.getNetworkOperatorName()
				+ "\",\"type\":\"" + telManager.getNetworkType()
				+ "\",\"active\":";
		if (activeNetwork != null) {
			msg += "\"" + activeNetwork.toString() + "\"";
		} else {
			msg += "null";
		}
		msg += "}";
		writeLine(msg);
	}

	/**
	 * 
	 * @param list
	 */
	public void writeListInfo(final VideoList list) {
		if (list != null) {
			writeLine("\"list\":{\"msg\":\"" + list.getCaptureMsg()
					+ "\",\"id\":\"" + list.getId() + "\",\"played\":"
					+ list.getPlayedCount() + "}");
		}
	}

	/**
	 * 
	 */
	public void logWifiRssi() {
		WifiInfo wifiInfo = ((WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
		writeLine("\"network\":{\"wifi_rssi\":" + wifiInfo.getRssi() + "}");
	}

	/**
	 * 
	 */
	public void writeVideoInfo(String vInfo) {
		writeLine("\"video\":{\"" + vInfo + "\"}");
	}

	/**
	 * 
	 */
	public void writeSysStats() {
		String files[] = { "/proc/stat", "/proc/meminfo" }; // /proc/version,
															// proc/cpuinfo
		String line;
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		writeLine("\"mem\":{\"avail\":" + mi.availMem + ",\"threshold\":"
				+ mi.threshold + "}");
		writeLine("\"time\":{\"currMillis\":"
				+ (System.currentTimeMillis() / 1000L) + ",\"elapsedRealtime\":"
				+ SystemClock.elapsedRealtime() + ",\"uptimeMillis\":"
				+ SystemClock.uptimeMillis() + "}");
		try {
			for (String file : files) {
				writeLine("\"" + file + "\":{\"count\":" + mSysInfoSaveCounter
						+ ",\"data\":[\"");
				try {
					RandomAccessFile reader = new RandomAccessFile(file, "r");
					while ((line = reader.readLine()) != null) {
						writeLine(line);
					}
					reader.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				writeLine("\"]}");
			}
			++mSysInfoSaveCounter;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		mCurrentThread = null;
	}

	@Override
	public void run() {
		Thread thisThread = Thread.currentThread();
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mListener.onLoggingStarted();
			}
		});

		while (mCurrentThread == thisThread) {
			try {
				mWriter.write(mMsgQueue.take());
				mWriter.flush();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			int pendingMsgs = mMsgQueue.size();
			for (int i = 0; i < pendingMsgs; ++i) {
				mWriter.write(mMsgQueue.remove());
			}
			mWriter.write("log_end");
			mWriter.close();
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					mListener.onLoggingStopped(true);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					mListener.onLoggingStopped(false);
				}
			});
		}
	}

}
