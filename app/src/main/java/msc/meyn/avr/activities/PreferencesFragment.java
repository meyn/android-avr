package msc.meyn.avr.activities;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

import msc.meyn.avr.AVRecorderApp;
import msc.meyn.avr.R;
import msc.meyn.avr.support.VideoList;
import msc.meyn.avr.support.VideoSite;

public class PreferencesFragment extends PreferenceFragment {

	private static final String LOGTAG = PreferencesFragment.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

//	@Override
//	public void onPause() {
//		super.onPause();
//		getPreferenceManager().getSharedPreferences()
//				.unregisterOnSharedPreferenceChangeListener(this);
//	}

	@Override
	public void onResume() {
		super.onResume();
		addTrainedSites(getActivity()); // dynamically add trained video sites
//		// now register for change notifications
//		getPreferenceManager().getSharedPreferences()
//				.registerOnSharedPreferenceChangeListener(this);
	}

//	@Override
//	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
//			String key) {
//		Log.d(LOGTAG, "onSharedPreferenceChanged(): " + key);
//	}

	// dynamically add trained video sites
	private void addTrainedSites(final Activity activity) {
		final PreferenceCategory trainedSitesCategory = (PreferenceCategory) findPreference("pref_key_trained_site_category");
		trainedSitesCategory.removeAll();
		final VideoList list = ((AVRecorderApp) getActivity().getApplication())
				.getCurrentList();
		if (list != null) {
			for (VideoSite trainedSite : list.getTrainedSites(activity)) {
				CheckBoxPreference chkBox = new CheckBoxPreference(activity);
				chkBox.setKey(trainedSite.getId());
				chkBox.setTitle(trainedSite.getTitle());
				chkBox.setChecked(false);
				trainedSitesCategory.addPreference(chkBox);
				// on checking the box delete this site's data
				chkBox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						try {
							VideoSite site = new VideoSite(preference.getKey(),
									activity);
							site.unwriteAutoPlayData(activity);
							site = null;
							trainedSitesCategory.removePreference(preference);
						} catch (java.lang.InstantiationException e) {
							e.printStackTrace();
							Log.e(LOGTAG, e.getMessage());
						}
						return true;
					}
				});
			}
		}
	}

}
