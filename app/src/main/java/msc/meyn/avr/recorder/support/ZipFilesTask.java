package msc.meyn.avr.recorder.support;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipFilesTask extends AsyncTask<String, String, String> {
	
	public interface TaskNotifier {
		public void onZipCompleted(String result);
		public void onZipProgress(String msg);
		public void onZipError(String errMsg);
	}
	
	private final static int ZIP_BUFFER_SIZE = 1024;
	private final TaskNotifier notifier;
	private final String baseDir;
	private final String fileNameStart;
	
	public ZipFilesTask(TaskNotifier notifier, String baseDir, String fileNameStart) {
		this.notifier = notifier;
		this.baseDir = baseDir;
		this.fileNameStart = fileNameStart;
	}
	
	@Override
	protected String doInBackground(String... zipFileName) {
		// find all files first
		File root = new File(baseDir);
		File capturedFiles[];
		
		final class FilteredFiles implements FilenameFilter {
			final String prefix;

			FilteredFiles(String prefix) {
				this.prefix = prefix;
			}
			
			@Override
			public boolean accept(File dir, String filename) {
				return filename.startsWith(prefix);
			}
		}
		publishProgress("Gathering files");
		capturedFiles = root.listFiles(new FilteredFiles(fileNameStart));
		
		if (capturedFiles.length > 0 ) {
			try {
				zipFiles(capturedFiles, zipFileName[0]);
				return zipFileName[0];
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private void zipFiles(File[] files, String zipFile) throws IOException {
	    BufferedInputStream origin = null;
	    ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(baseDir + "/" + zipFile)));
	    String filePath;
	    File currentFile;
        int count;
        Log.d("ZipFileTask", "zipping " + zipFile + " with: " + Arrays.toString(files));
	    
	    try { 
	        byte data[] = new byte[ZIP_BUFFER_SIZE];
	        for (int i = 0; i < files.length; i++) {
	        	filePath = files[i].getAbsolutePath();
	            FileInputStream fi = new FileInputStream(filePath);
	            Log.d("ZipFileTask", "zip adding file: " + filePath);
	            publishProgress("compressing " + files[i].getName());
	            origin = new BufferedInputStream(fi, ZIP_BUFFER_SIZE);
                ZipEntry entry = new ZipEntry(filePath.substring(filePath.lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                while ((count = origin.read(data, 0, ZIP_BUFFER_SIZE)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
                currentFile = new File(filePath);
                currentFile.delete();
	        }
	    }
	    finally {
	        out.close();
	    }
	}
	
	@Override
	protected void onProgressUpdate(String... progress) {
		if (notifier != null) {
			notifier.onZipProgress(progress[0]);
		}
	}
	
	@Override
	protected void onPostExecute(String result) {
		if (notifier != null) {
			notifier.onZipCompleted(result);
		}
	}

}