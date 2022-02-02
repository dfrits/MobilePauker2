package de.daniel.mobilepauker2.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.utils.Constants
import javax.inject.Inject

class SettingsFragmentNotifications : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (context?.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        addPreferencesFromResource(R.xml.preferences_notifications)
        val preferenceScreen: PreferenceScreen = preferenceScreen
        preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
        init(preferenceScreen)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePrefSummary(findPreference(key!!))
    }

    override fun onResume() {
        super.onResume()
        updatePrefSummary(findPreference(settingsManager.getSettingsKey(SettingsManager.Keys.RING_TONE)))
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun init(preference: Preference) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                init(preference.getPreference(i))
            }
        } else {
            updatePrefSummary(preference)
        }
        findPreference<Preference>(settingsManager.getSettingsKey(SettingsManager.Keys.RING_TONE))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context?.packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_ID)
                startActivity(intent)
                true
            }
    }

    private fun updatePrefSummary(preference: Preference?) {
        preference?.key?.let { preferenceKey ->
            if (preferenceKey == settingsManager.getSettingsKey(SettingsManager.Keys.RING_TONE)) {
                val notificationManager =
                    context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ringtonePath =
                    notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)?.sound
                ringtonePath?.let {
                    val ringtone = RingtoneManager.getRingtone(context, it)
                    preference.summary = ringtone.getTitle(context)
                }
            }
        }
    }
}