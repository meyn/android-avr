package msc.meyn.avr.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.activities.TrainingController;
import msc.meyn.avr.support.VideoItem;
import msc.meyn.avr.support.VideoList;

public class HandleSchedulingIS extends IntentService {

	private final static String CLASS_NAME = HandleSchedulingIS.class
			.getName();

	public HandleSchedulingIS() {
		super(CLASS_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v(CLASS_NAME, "onHandleIntent(): " + intent);
		Context context = getApplicationContext();
		VideoList list = ((AVRecorderApp) context).getCurrentList();
		ArrayList<VideoItem> allVideos = list.getAllVideos();
		ArrayList<VideoItem> autoPlayableVideos = list
				.getTrainedVideoItems(context);

		if (allVideos.size() > 0) {
			if (allVideos.size() == autoPlayableVideos.size()) {
				startService(new Intent(context, PlayVideosIS.class)
						.setAction(PlayVideosIS.ACTION_AUTO_VIDEO_LIST_PLAY));
			} else {
				startActivity(new Intent(context, TrainingController.class)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}
		}
	}

}
