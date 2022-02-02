package de.daniel.mobilepauker2.shortcut

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.widget.Toast
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.models.view.TextDrawable
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class ShortcutsManager @Inject constructor(private val context: Context) {
    private val shortcutManager: ShortcutManager =
        context.getSystemService(ShortcutManager::class.java)

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var dataManager: DataManager

    fun createShortcut(activity: Activity, filename: String) {
        if (shortcutManager.dynamicShortcuts.size == 5) {
            Log.d("LessonImportActivity::createShortcut", "already 5 shortcuts created")
            toaster.showToast(activity, R.string.shortcut_create_error, Toast.LENGTH_LONG)
        } else {
            val intent = Intent(context, ShortcutReceiver::class.java)
            intent.action = Constants.SHORTCUT_ACTION
            intent.putExtra(Constants.SHORTCUT_EXTRA, filename)
            val icon = TextDrawable(filename[0].toString())
            icon.setBold(true)
            val shortcut = ShortcutInfo.Builder(context, filename)
                .setShortLabel(dataManager.getReadableFileName(filename))
                .setIcon(Icon.createWithBitmap(drawableToBitmap(icon)))
                .setIntent(intent)
                .build()
            shortcutManager.addDynamicShortcuts(listOf(shortcut))
            toaster.showToast(activity, R.string.shortcut_added, Toast.LENGTH_SHORT)
            Log.d("LessonImportActivity::createShortcut", "Shortcut created")
        }
    }

    fun deleteShortcut(activity: Activity, ID: String) {
        shortcutManager.removeDynamicShortcuts(listOf(ID))
        toaster.showToast(activity, R.string.shortcut_removed, Toast.LENGTH_SHORT)
        Log.d("LessonImportActivity::deleteShortcut", "Shortcut deleted")
    }

    fun hasShortcut(ID: String): Boolean {
        val shortcuts = shortcutManager.dynamicShortcuts
        for (info in shortcuts) {
            if (info.id == ID) {
                return true
            }
        }
        return false
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}