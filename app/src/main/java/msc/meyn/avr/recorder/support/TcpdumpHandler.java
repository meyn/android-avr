package msc.meyn.avr.recorder.support;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class TcpdumpHandler {

	private static final String CLASS_NAME = TcpdumpHandler.class.getName();

	private final String mExeName;
	private final String mExeBasePath;
	private final int mSnaplen;
	private final String mOutputFileFullPath;

	private static Shell.Interactive mSuShellSession;
	private boolean mTcpdumpStarted;

	private static final int PS_TCPDUMP_CMD_CODE = 100;
	private static final int PS_KILL_TCPDUMP_CMD_CODE = 101;
	private static final int TCPDUMP_INTERNAL_CMD_CODE = 102;

	public interface Listener {
		public void onTcpdumpStartError(int exitCode);

		public void onTcpdumpStarted();

		public void onTcpdumpStopError(int exitCode);

		public void onTcpdumpStopped();

		public void onTcpdumpReport(String msg);
	}
	
	private interface PostKillTcpdump {
		public void onCompleted();
	}

	private Listener mListener;

	public TcpdumpHandler(Listener listener, String tcpdumpExeName,
			String basePathToExe, int snaplen, String outputFile) {
		if (listener == null) {
			throw new IllegalArgumentException("invalid listener");
		}
		mListener = listener;
		mExeName = tcpdumpExeName;
		mExeBasePath = basePathToExe;
		mSnaplen = snaplen;
		mOutputFileFullPath = outputFile;
		mTcpdumpStarted = false;
	}

	public void startTcpdump() {
		openSuShell();
	}

	public void stopTcpdump() {
		if (mTcpdumpStarted) {
			killallTcpdumpProcesses(new PostKillTcpdump() {
				@Override
				public void onCompleted() {
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
							mListener.onTcpdumpStopped();
							mTcpdumpStarted = false;
						}
					});
				}
			});
		} else {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					mListener.onTcpdumpStopped();
				}
			});
		}
	}

	private void openSuShell() {
		mSuShellSession = new Shell.Builder().useSU().setWantSTDERR(true)
				.setWatchdogTimeout(5).setMinimalLogging(true)
				.open(mOnShellStartResult);
	}

	private void onShellOpenError(final int exitCode) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mListener.onTcpdumpStartError(exitCode);
			}
		});
	}

	private void killallTcpdumpProcesses(final PostKillTcpdump killAllTcpdump) {
		mSuShellSession.addCommand(new String[] { "ps " + mExeName },
				PS_TCPDUMP_CMD_CODE, new Shell.OnCommandResultListener() {
					@Override
					public void onCommandResult(int commandCode, int exitCode,
							List<String> output) {
						if (exitCode >= 0) {
							int pidColumn = Arrays.asList(
									output.get(0).split(" +")).indexOf("PID");
							if (pidColumn != -1) {
								output.remove(0);
								for (String psOutputLine : output) {
									String pid = Arrays.asList(
											psOutputLine.split(" +")).get(
											pidColumn);
									Log.d(CLASS_NAME, "EXECUTING: "
											+ "kill -15 " + pid);
									mSuShellSession
											.addCommand(new String[] { "kill -15 "
													+ pid });
								}
								killAllTcpdump.onCompleted();
							}
						}
					}
				});
	}

	private void executeTcpdumpCmd() {
		String[] tcpdumpCmds = {
				"./" + mExeBasePath + "/" + mExeName + " -n -s " + mSnaplen
						+ " -w " + mOutputFileFullPath
						+ " tcp > /dev/null 2>&1&", "echo $!" };
		mSuShellSession.addCommand(tcpdumpCmds, TCPDUMP_INTERNAL_CMD_CODE,
				mOnTcpdumpStartResult);
	}

	private Shell.OnCommandResultListener mOnShellStartResult = new Shell.OnCommandResultListener() {
		@Override
		public void onCommandResult(int commandCode, int exitCode,
				List<String> output) {
			if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
				onShellOpenError(exitCode);
			} else {
				killallTcpdumpProcesses(new PostKillTcpdump() {
					@Override
					public void onCompleted() {
						executeTcpdumpCmd();
					}
				});
			}
		}
	};

	private Shell.OnCommandResultListener mOnTcpdumpStartResult = new Shell.OnCommandResultListener() {
		@Override
		public void onCommandResult(int commandCode, final int exitCode,
				List<String> output) {
			if (exitCode < 0) {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						mListener.onTcpdumpStartError(exitCode);
					}
				});
			} else {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						mListener.onTcpdumpStarted();
						mTcpdumpStarted = true;
					}
				});
			}
		}
	};

}
