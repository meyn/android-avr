package msc.meyn.avr.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class VideoListSelectDialog extends DialogFragment {
	private static final String KEY_TITLES = "TITLES";

	static VideoListSelectDialog newInstance(CharSequence[] titles) {
		VideoListSelectDialog inst = new VideoListSelectDialog();
		Bundle args = new Bundle();
		args.putCharSequenceArray(KEY_TITLES, titles);
		inst.setArguments(args);
		return inst;
	}

	public interface Listener {
		public void onDialogNegativeClick(DialogFragment dialog);

		public void onDialogListChosen(DialogFragment dialog, int index);
	}

	Listener mListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (Listener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ListChooserDialogListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		CharSequence[] titles = getArguments().getCharSequenceArray(KEY_TITLES);
		builder.setTitle("Select Playlist")
				.setSingleChoiceItems(titles, -1,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mListener
										.onDialogListChosen(VideoListSelectDialog.this, which);
								dismiss();
							}
						})
				.setNegativeButton("More",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mListener
										.onDialogNegativeClick(VideoListSelectDialog.this);
							}
						});
		return builder.create();
	}

}
