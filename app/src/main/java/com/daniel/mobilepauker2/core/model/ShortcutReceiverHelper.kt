package com.daniel.mobilepauker2.core.model

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
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.core.model.ui.TextDrawable
import com.daniel.mobilepauker2.main.ShortcutReceiver
import com.daniel.mobilepauker2.pauker_native.Log
import org.koin.core.KoinComponent
import org.koin.core.get

class ShortcutReceiverHelper : KoinComponent {
    /**
     * Erstellt einen Shortcut und fügt diesen hinzu.
     * @param filename Name der Lektion, von der ein Shortcut erstellt werden soll
     */
    fun createShortcut(context: Context, filename: String) {
        val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
        )
        if (shortcutManager != null) {
            if (shortcutManager.dynamicShortcuts.size == 5) {
                Log.d(
                        "LessonImportActivity::createShortcut",
                        "already 5 shortcuts created"
                )
                PaukerManager.showToast(
                        context as Activity,
                        R.string.shortcut_create_error,
                        Toast.LENGTH_LONG
                )
            } else {
                val intent = Intent(context, ShortcutReceiver::class.java)
                intent.action = Constants.SHORTCUT_ACTION
                intent.putExtra(
                        Constants.SHORTCUT_EXTRA,
                        filename
                )
                val icon = TextDrawable(filename[0].toString())
                icon.setBold(true)
                val paukerManager = get<PaukerManager>()
                val readableName = paukerManager.getReadableFileName(filename)
                if (readableName != null) {
                    val shortcut = ShortcutInfo.Builder(context, filename)
                            .setShortLabel(readableName)
                            .setIcon(Icon.createWithBitmap(drawableToBitmap(icon)))
                            .setIntent(intent)
                            .build()
                    shortcutManager.addDynamicShortcuts(listOf(shortcut))
                    PaukerManager.showToast(
                            context as Activity,
                            R.string.shortcut_added,
                            Toast.LENGTH_SHORT
                    )
                    Log.d(
                            "LessonImportActivity::createShortcut",
                            "Shortcut created"
                    )
                } else {
                    PaukerManager.showToast(
                            context as Activity,
                            R.string.shortcut_create_error,
                            Toast.LENGTH_SHORT
                    )
                    Log.d(
                            "LessonImportActivity::createShortcut",
                            "Shortcut not created"
                    )
                }
            }
        }
    }

    fun deleteShortcut(context: Context, ID: String) {
        val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
        )
        if (shortcutManager != null) {
            shortcutManager.removeDynamicShortcuts(listOf(ID))
            PaukerManager.showToast(
                    context as Activity,
                    R.string.shortcut_removed,
                    Toast.LENGTH_SHORT
            )
            Log.d(
                    "LessonImportActivity::deleteShortcut",
                    "Shortcut deleted"
            )
        }
    }

    /**
     * Wandelt das Drawable in ein Bitmap um.
     * @param drawable TextDrawable
     * @return Bitmap
     */
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

    fun hasShortcut(context: Context, ID: String): Boolean {
        val shortcutManager = context.getSystemService(
                ShortcutManager::class.java
        )
        if (shortcutManager != null) {
            val shortcuts =
                    shortcutManager.dynamicShortcuts
            for (info in shortcuts) {
                if (info.id == ID) {
                    return true
                }
            }
        }
        return false
    }
}