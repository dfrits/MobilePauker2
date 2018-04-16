package com.daniel.mobilepauker2.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.MinFilter;
import com.dropbox.core.android.Auth;

import static com.daniel.mobilepauker2.model.SettingsManager.Keys.AUTO_SYNC;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.FLIP_CARD_SIDES;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.FONT_SIZE;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.REPEAT_CARDS;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.RETURN_FORGOTTEN_CARDS;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.START_ASSOCIATION;
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
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        init(preferenceScreen);
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
    }

    private void updatePrefSummary(Preference preference) {
        if (preference == null) return;

        CharSequence summ = preference.getSummary();
        Context context = getContext();

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
                } else if (s.equals(settingsManager.getSettingsKey(context, FONT_SIZE))) {
                    summ = getString(R.string.font_size_sum);
                }

                listP.setSummary(String.format(summ.toString(), listP.getEntry()));
            }
        }

        if (preference.getKey() != null
                && preference.getKey().equals(settingsManager.getSettingsKey(context, START_ASSOCIATION))) {

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String accessToken = pref.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
            if (accessToken == null) {
                accessToken = Auth.getOAuth2Token();
                if (accessToken != null) {
                    pref.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply();
                    initSyncPrefs(preference);
                } else {
                    preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Auth.startOAuth2Authentication(getActivity(), Constants.DROPBOX_APP_KEY);
                            return false;
                        }
                    });
                }
            } else {
                getPreferenceScreen().removePreference(preference);
                initSyncPrefs(preference);
            }
        }
    }

    private void initSyncPrefs(Preference preference) {
        ((PreferenceCategory) findPreference(getString(R.string.other))).removePreference(preference);
        SwitchPreference switchP = (SwitchPreference) findPreference(settingsManager
                .getSettingsKey(getContext(), AUTO_SYNC));

        switchP.setSummary(R.string.auto_sync_enabled_summ);
        switchP.setEnabled(true);
    }
}
