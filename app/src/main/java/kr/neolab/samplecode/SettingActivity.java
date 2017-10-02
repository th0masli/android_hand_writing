package kr.neolab.samplecode;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

public class SettingActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private EditTextPreference mURL;
	private CheckBoxPreference mOfflineStatus;

	private SharedPreferences sp = null;
	public static final String app_setting_tag = "app_olhw_setting_tag";
	public static final String URL_TAG = "url";
	public static final String OFFLINEMODE_TAG = "offline_status_tag";

	public static final int LAYOUTMODE_FREE = 0;
	public static final int LAYOUTMODE_PIGAI = 1;
    public static final String LAYOUTMODE_TAG = "layout_mode_tag";

	public static final String defaultURL = "http://106.75.26.145:5000";
	public static final String localAPI = "local";
	public static final boolean defaultOfflineMode = false;
    public static final int defaultLayoutMode = LAYOUTMODE_PIGAI;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_settings);

		sp = getSharedPreferences(app_setting_tag, Context.MODE_PRIVATE);
		final String url = sp.getString(URL_TAG,defaultURL);
		final boolean offlineMode = sp.getBoolean(OFFLINEMODE_TAG,defaultOfflineMode);
        final int layoutMode = sp.getInt(LAYOUTMODE_TAG,defaultLayoutMode);

		mURL = (EditTextPreference)findPreference(URL_TAG);
		if(mURL != null) {
			mURL.setText(url);
		}
		mOfflineStatus = (CheckBoxPreference)findPreference(OFFLINEMODE_TAG);
		if(mOfflineStatus !=null) {
			mOfflineStatus.setChecked(offlineMode);
		}

		SharedPreferences.Editor editor = sp.edit();
		editor.putString(URL_TAG,url);
		editor.putBoolean(OFFLINEMODE_TAG,offlineMode);
        editor.putInt(LAYOUTMODE_TAG,layoutMode);
		editor.apply();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(URL_TAG)) {
			String new_state = sharedPreferences.getString(URL_TAG,defaultURL);
			if (new_state.length() > 0) {
				if (new_state.substring(new_state.length()-1,new_state.length()).equals("/")){
					new_state = new_state.substring(0,new_state.length()-1);
				}
				SharedPreferences.Editor editor = sp.edit();
				editor.putString(URL_TAG, new_state);
				editor.apply();
			}
		}else if (key.equals(OFFLINEMODE_TAG)) {
			final boolean new_state = sharedPreferences.getBoolean(OFFLINEMODE_TAG,defaultOfflineMode);
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean(OFFLINEMODE_TAG,new_state);
			editor.apply();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
}
