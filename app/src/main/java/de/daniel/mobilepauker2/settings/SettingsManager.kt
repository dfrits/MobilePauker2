package de.daniel.mobilepauker2.settings

import android.content.Context
import androidx.annotation.NonNull
import androidx.preference.PreferenceManager
import de.daniel.mobilepauker2.R
import javax.inject.Inject

class SettingsManager @Inject constructor(val context: Context) {

    @NonNull
    fun getSettingsKey(key: Keys?): String {
        return when (key) {
            Keys.STM -> context.getString(R.string.stm_key)
            Keys.USTM -> context.getString(R.string.ustm_key)
            Keys.ABOUT -> context.getString(R.string.about)
            Keys.AUTO_UPLOAD -> context.getString(R.string.auto_upload)
            Keys.HIDE_TIMES -> context.getString(R.string.hide_times)
            Keys.REPEAT_CARDS -> context.getString(R.string.repeat_cards_mode)
            Keys.CASE_SENSITIV -> context.getString(R.string.case_sensitive)
            Keys.FLIP_CARD_SIDES -> context.getString(R.string.flip_card_sides)
            Keys.DB_PREFERENCE -> context.getString(R.string.associate_dropbox)
            Keys.RETURN_FORGOTTEN_CARDS -> context.getString(R.string.return_forgotten_cards)
            Keys.LEARN_NEW_CARDS_RANDOMLY -> context.getString(R.string.learn_new_cards_randomly)
            Keys.ENABLE_EXPIRE_TOAST -> context.getString(R.string.expire_toast)
            Keys.SHOW_TIMER_NOTIFY -> context.getString(R.string.show_timer_notification)
            Keys.SHOW_CARD_NOTIFY -> context.getString(R.string.show_card_notification)
            Keys.RING_TONE -> context.getString(R.string.ring_tone_preference)
            Keys.SHOW_TIMER_BAR -> context.getString(R.string.show_timer_bar)
            else -> ""
        }
    }

    @NonNull
    private fun getDefaultStringValue(key: Keys?): String {
        return when (key) {
            Keys.STM -> context.getString(R.string.stm_default)
            Keys.USTM -> context.getString(R.string.ustm_default)
            Keys.REPEAT_CARDS -> context.getString(R.string.repeat_cards_default)
            Keys.FLIP_CARD_SIDES -> context.getString(R.string.flip_card_sides_default)
            Keys.RETURN_FORGOTTEN_CARDS -> context.getString(R.string.return_forgotten_cards_default)
            else -> ""
        }
    }

    private fun getDefaultBoolValue(key: Keys?): Boolean {
        return when (key) {
            Keys.HIDE_TIMES -> context.resources.getBoolean(R.bool.hide_times_default)
            Keys.AUTO_UPLOAD -> context.resources.getBoolean(R.bool.auto_upload_default)
            Keys.CASE_SENSITIV -> context.resources.getBoolean(R.bool.case_sensitive_default)
            Keys.LEARN_NEW_CARDS_RANDOMLY -> context.resources.getBoolean(R.bool.learn_new_cards_randomly_default)
            Keys.ENABLE_EXPIRE_TOAST -> context.resources.getBoolean(R.bool.expire_toast_default)
            Keys.SHOW_TIMER_NOTIFY -> context.resources.getBoolean(R.bool.show_timer_notification_default)
            Keys.SHOW_CARD_NOTIFY -> context.resources.getBoolean(R.bool.show_card_notification_default)
            Keys.SHOW_TIMER_BAR -> context.resources.getBoolean(R.bool.show_timer_bar_default)
            else -> false
        }
    }

    fun getBoolPreference(key: Keys): Boolean {
        val prefKey = getSettingsKey(key)
        val defValue = getDefaultBoolValue(key)
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(prefKey, defValue)
    }

    fun getStringPreference(key: Keys?): String? {
        val prefKey = getSettingsKey(key)
        val defValue = getDefaultStringValue(key)
        return PreferenceManager.getDefaultSharedPreferences(context).getString(prefKey, defValue)
    }

    enum class Keys {
        ABOUT,
        CASE_SENSITIV,
        AUTO_UPLOAD,
        DB_PREFERENCE,
        ENABLE_EXPIRE_TOAST,
        FLIP_CARD_SIDES,
        HIDE_TIMES,
        LEARN_NEW_CARDS_RANDOMLY,
        REPEAT_CARDS,
        RETURN_FORGOTTEN_CARDS,
        RING_TONE,
        SHOW_CARD_NOTIFY,
        SHOW_TIMER_BAR,
        SHOW_TIMER_NOTIFY,
        STM,
        USTM
    }
}