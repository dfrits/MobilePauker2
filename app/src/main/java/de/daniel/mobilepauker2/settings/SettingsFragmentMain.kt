package de.daniel.mobilepauker2.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings.*
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.*
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.MinFilter
import javax.inject.Inject

class SettingsFragmentMain : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var dialogFragment: DialogFragment? = null
    private val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (context?.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
        addPreferencesFromResource(R.xml.preferences_main)
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
        updatePrefSummary(findPreference(settingsManager.getSettingsKey(RING_TONE)))
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (activity is PaukerSettings) {
            val dialog: DialogFragment = when (preference) {
                is EditTextPreference -> {
                    EditTextPreferenceDialogFragmentCompat.newInstance(preference.key)
                }
                is ListPreference -> {
                    ListPreferenceDialogFragmentCompat.newInstance(preference.key)
                }
                else -> {
                    throw IllegalArgumentException(
                        "Tried to display dialog for unknown " +
                            "preference type. Did you forget to override onDisplayPreferenceDialog()?"
                    )
                }
            }

            if (preference is EditTextPreference) {
                preference.setOnBindEditTextListener { editText ->
                    editText.addTextChangedListener(MinFilter(dialog))
                }
            }

            dialog.setTargetFragment(this, 0)

            dialogFragment = dialog

            dialog.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun init(preference: Preference) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                init(preference.getPreference(i))
            }
        } else {
            updatePrefSummary(preference)
        }
        findPreference<Preference>(settingsManager.getSettingsKey(RING_TONE))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(EXTRA_APP_PACKAGE, context?.packageName)
                intent.putExtra(EXTRA_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_ID)
                startActivity(intent)
                true
            }
    }

    private fun updatePrefSummary(preference: Preference?) {
        preference?.summary?.let { summ ->
            var newSumm = summ

            if (preference is EditTextPreference) {
                if (preference.key == settingsManager.getSettingsKey(USTM))
                    newSumm = getString(R.string.ustm_summ)
                else if (preference.key == settingsManager.getSettingsKey(STM))
                    newSumm = getString(R.string.stm_summ)
                preference.summary = String.format(newSumm.toString(), preference.text)
            } else if (preference is ListPreference) {
                when (preference.key) {
                    settingsManager.getSettingsKey(REPEAT_CARDS) -> {
                        newSumm = getString(R.string.repeat_cards_summ)
                    }
                    settingsManager.getSettingsKey(RETURN_FORGOTTEN_CARDS) -> {
                        newSumm = getString(R.string.return_forgotten_cards_summ)
                    }
                    settingsManager.getSettingsKey(FLIP_CARD_SIDES) -> {
                        newSumm = getString(R.string.flip_card_sides_summ)
                    }
                }
                preference.summary = String.format(newSumm.toString(), preference.entry)
            }
        }
    }
}