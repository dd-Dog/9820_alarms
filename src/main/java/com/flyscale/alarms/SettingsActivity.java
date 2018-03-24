package com.flyscale.alarms;

import com.flyscale.alarms.utils.DLog;
import com.flyscale.alarms.widget.SnoozeLengthDialog;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "SettingsActivity";
    public static final String KEY_AUTO_SILENCE = "auto_silence";
    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_VOLUME_BUTTONS = "volume_button_settings";

    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    public static final String DEFAULT_SNOOZE_MINUTES = "10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
                    ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        ListPreference listPref = (ListPreference) findPreference(KEY_AUTO_SILENCE);
        String delay = listPref.getValue();
        updateAutoSnoozeSummary(listPref, delay);
        listPref.setOnPreferenceChangeListener(this);

        SnoozeLengthDialog snoozePref = (SnoozeLengthDialog) findPreference(KEY_ALARM_SNOOZE);
        snoozePref.setSummary();

        listPref = (ListPreference) findPreference(KEY_VOLUME_BUTTONS);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        DLog.d(TAG, "onPreferenceChange()");
        if (KEY_AUTO_SILENCE.equals(preference.getKey())) {
            final ListPreference listPref = (ListPreference) preference;
            String delay = (String) newValue;
            updateAutoSnoozeSummary(listPref, delay);
        } else if (KEY_VOLUME_BUTTONS.equals(preference.getKey())) {
            final ListPreference listPref = (ListPreference) preference;
            final int index = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[index]);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
        int i = Integer.parseInt(delay);
        if (i == -1) {
            listPref.setSummary(R.string.auto_silence_never);
        } else {
            listPref.setSummary(getString(R.string.auto_silence_summary, i));
        }
    }

}
