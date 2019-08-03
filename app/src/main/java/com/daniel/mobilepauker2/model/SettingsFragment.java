package com.daniel.mobilepauker2.model;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.DropboxAccDialog;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;
import com.daniel.mobilepauker2.utils.MinFilter;

import static android.app.Activity.RESULT_OK;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.AUTO_DOWNLOAD;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.AUTO_UPLOAD;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.DB_PREFERENCE;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.FLIP_CARD_SIDES;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.REPEAT_CARDS;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.RETURN_FORGOTTEN_CARDS;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.RING_TONE;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.STM;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.USTM;

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final SettingsManager settingsManager = SettingsManager.instance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        init(preferenceScreen);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == RESULT_OK) {
            initSyncPrefs(getContext());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePrefSummary(findPreference(settingsManager.getSettingsKey(getContext(), RING_TONE)));
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void init(Preference preference) {
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) preference;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                init(pGrp.getPreference(i));
            }
        } else {
            if (preference instanceof EditTextPreference) {
                EditTextPreference editTextP = (EditTextPreference) preference;
                editTextP.getEditText().addTextChangedListener(new MinFilter(editTextP));
            }
            updatePrefSummary(preference);
        }
        findPreference(settingsManager.getSettingsKey(getContext(), RING_TONE)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_ID);
                startActivity(intent);
                return true;
            }
        });
    }

    private void updatePrefSummary(Preference preference) {
        if (preference == null) return;

        CharSequence summ = preference.getSummary();
        final Context context = getContext();

        if (summ != null) {
            if (preference instanceof EditTextPreference) {
                EditTextPreference editTextP = (EditTextPreference) preference;

                if (editTextP.getKey().equals(settingsManager.getSettingsKey(context, USTM)))
                    summ = getString(R.string.ustm_summ);
                else if (editTextP.getKey().equals(settingsManager.getSettingsKey(context, STM)))
                    summ = getString(R.string.stm_summ);

                editTextP.setSummary(String.format(summ.toString(), editTextP.getText()));
            } else if (preference instanceof ListPreference) {
                ListPreference listP = (ListPreference) preference;

                String s = listP.getKey();
                if (s.equals(settingsManager.getSettingsKey(context, REPEAT_CARDS))) {
                    summ = getString(R.string.repeat_cards_summ);
                } else if (s.equals(settingsManager.getSettingsKey(context, RETURN_FORGOTTEN_CARDS))) {
                    summ = getString(R.string.return_forgotten_cards_summ);
                } else if (s.equals(settingsManager.getSettingsKey(context, FLIP_CARD_SIDES))) {
                    summ = getString(R.string.flip_card_sides_summ);
                }

                listP.setSummary(String.format(summ.toString(), listP.getEntry()));
            }
        }

        String preferenceKey = preference.getKey();
        if (preferenceKey != null) {
            if (preferenceKey.equals(settingsManager.getSettingsKey(context, DB_PREFERENCE))) {
                initSyncPrefs(context);
            } else if (preferenceKey.equals(settingsManager.getSettingsKey(context, RING_TONE))) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    Uri ringtonePath = notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID).getSound();
                    Ringtone ringtone = RingtoneManager.getRingtone(context, ringtonePath);
                    preference.setSummary(ringtone.getTitle(context));
                } else {
                    preference.setSummary("No Sound");
                }
            }
        }
    }

    private void removeSyncPrefAndSetAutoSync(boolean enableAutoSync) {
        SwitchPreference switchUp = (SwitchPreference) findPreference(settingsManager
                .getSettingsKey(getContext(), AUTO_UPLOAD));
        SwitchPreference switchDown = (SwitchPreference) findPreference(settingsManager
                .getSettingsKey(getContext(), AUTO_DOWNLOAD));

        if (enableAutoSync) {
            switchUp.setSummary(R.string.auto_sync_enabled_upload_summ);
            switchDown.setSummary(R.string.auto_sync_enabled_download_summ);
        } else {
            switchUp.setSummary(R.string.auto_sync_disabled_summ);
            switchDown.setSummary(R.string.auto_sync_disabled_summ);
        }

        switchUp.setEnabled(enableAutoSync);
        switchDown.setEnabled(enableAutoSync);
    }

    private void initSyncPrefs(final Context context) {
        Log.d("SettingsFragment::initSyncPrefs", "init syncprefs");
        SettingsManager instance = SettingsManager.instance();
        Preference dbPref = findPreference(instance.getSettingsKey(context, DB_PREFERENCE));
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final String accessToken = pref.getString(Constants.DROPBOX_ACCESS_TOKEN, null);

        if (accessToken == null) {
            setPrefAss(context, dbPref);
        } else {
            pref.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply();
            setPrefUnlink(context, dbPref);
            Log.d("SettingsFragment::initSyncPrefs", "enable autosync");
            removeSyncPrefAndSetAutoSync(true);
        }
    }

    private void setPrefAss(Context context, Preference dbPref) {
        dbPref.setTitle(R.string.associate_dropbox_title);
        final Intent assIntent = new Intent(context, DropboxAccDialog.class);
        assIntent.putExtra(DropboxAccDialog.AUTH_MODE, true);
        dbPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(assIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG);
                return false;
            }
        });
        Log.d("SettingsFragment::initSyncPrefs", "disable autosync");
        removeSyncPrefAndSetAutoSync(false);
    }

    private void setPrefUnlink(final Context context, final Preference dbPref) {
        dbPref.setTitle(R.string.unlink_dropbox_title);
        final Intent unlIntent = new Intent(context, DropboxAccDialog.class);
        unlIntent.putExtra(DropboxAccDialog.UNL_MODE, true);
        dbPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d("SettingsFragment::initSyncPrefs", "unlinkDB clicked");
                startActivityForResult(unlIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG);
                return false;
            }
        });
    }
}
