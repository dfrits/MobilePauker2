package com.daniel.mobilepauker2.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.SyncDialog;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.model.TextDrawable;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.daniel.mobilepauker2.utils.Constants.SHORTCUT_EXTRA;

public class ShortcutReceiver extends Activity {
    private final PaukerManager paukerManager = PaukerManager.instance();
    private final Context context = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);
        RelativeLayout progressBar = findViewById(R.id.pFrame);
        progressBar.setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.pTitle);
        title.setText(R.string.open_lesson_hint);

        Intent intent = getIntent();
        if (Constants.SHORTCUT_ACTION.equals(intent.getAction())) {
            openLesson(intent);
        }
        //finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println();
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN) {
            if (resultCode == RESULT_OK) {
                Log.d("ShortcutReceiver::onActivityResult", "File wurde aktualisiert");
            } else {
                Log.d("ShortcutReceiver::onActivityResult", "File wurde nicht aktualisiert");
            }
            String filename = getIntent().getStringExtra(SHORTCUT_EXTRA);
            if (filename != null) {
                openLesson(filename);
            } else {
                Log.d("ShortcutReceiver::onActivityResult","Filename is null");
            }
        }
    }

    /**
     * Handelt den Shortcut und öffnet die entsprechende Lektion.
     * @param shortcutIntent Intent vom Shortcut.
     */
    private void openLesson(Intent shortcutIntent) {
        if (LearnCardsActivity.isLearningRunning()) {
            PaukerManager.showToast((Activity) context, R.string.shortcut_open_error_learning_running, Toast.LENGTH_SHORT);
            return;
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            PaukerManager.showToast((Activity) context, R.string.shortcut_open_error_permission, Toast.LENGTH_SHORT);
            return;
        }

        String filename = shortcutIntent.getStringExtra(SHORTCUT_EXTRA);
        if (filename == null) {
            return;
        }

        if (SettingsManager.instance().getBoolPreference(context, SettingsManager.Keys.AUTO_SYNC)) {
            Log.d("ShortcutReceiver::openLesson", "Check for newer version on DB");
            String accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.DROPBOX_ACCESS_TOKEN, null);
            Intent syncIntent = new Intent(context, SyncDialog.class);
            try {
                syncIntent.putExtra(SyncDialog.FILES, paukerManager.getFilePath(context, filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
            syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken);
            syncIntent.setAction(SyncDialog.SYNC_FILE_ACTION);
            PaukerManager.showToast((Activity) context, "ShortcutReceiver::openLesson", Toast.LENGTH_LONG);
            startActivityForResult(syncIntent, Constants.REQUEST_CODE_SYNC_DIALOG_BEFORE_OPEN);
        } else {
            openLesson(filename);
        }
    }

    /**
     * Öffnet die Lektion und beendet anschließend die Activity.
     * @param filename Name der Lektion
     */
    private void openLesson(String filename) {
        try {
            if (!paukerManager.getCurrentFileName().equals(filename)) {
                paukerManager.loadLessonFromFile(paukerManager.getFilePath(context, filename));
                paukerManager.setSaveRequired(false);
            }

            Intent intent = new Intent(context, MainMenu.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (IOException e) {
            PaukerManager.showToast((Activity) context, getString(R.string.error_reading_from_xml), Toast.LENGTH_SHORT);
            ErrorReporter.instance().AddCustomData("ImportThread", "IOException?");
        }
    }

    /**
     * Erstellt einen Shortcut und fügt diesen hinzu.
     * @param filename Name der Lektion, von der ein Shortcut erstellt werden soll
     */
    public static void createShortcut(Context context, String filename) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (shortcutManager != null) {
            if (shortcutManager.getDynamicShortcuts().size() == 5) {
                Log.d("LessonImportActivity::createShortcut", "already 5 shortcuts created");
                PaukerManager.showToast((Activity) context, R.string.shortcut_create_error, Toast.LENGTH_LONG);
            } else {
                Intent intent = new Intent(context, ShortcutReceiver.class);
                intent.setAction(Constants.SHORTCUT_ACTION);
                intent.putExtra(SHORTCUT_EXTRA, filename);
                TextDrawable icon = new TextDrawable(String.valueOf(filename.charAt(0)));
                icon.setBold(true);
                ShortcutInfo shortcut = new ShortcutInfo.Builder(context, filename)
                        .setShortLabel(PaukerManager.instance().getReadableFileName(filename))
                        .setIcon(Icon.createWithBitmap(drawableToBitmap(icon)))
                        .setIntent(intent)
                        .build();
                shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
                PaukerManager.showToast((Activity) context, R.string.shortcut_added, Toast.LENGTH_SHORT);
                Log.d("LessonImportActivity::createShortcut", "Shortcut created");
            }
        }
    }

    public static void deleteShortcut(Context context, String ID) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (shortcutManager != null) {
            shortcutManager.removeDynamicShortcuts(Collections.singletonList(ID));
            PaukerManager.showToast((Activity) context, R.string.shortcut_removed, Toast.LENGTH_SHORT);
            Log.d("LessonImportActivity::deleteShortcut", "Shortcut deleted");
        }
    }

    /**
     * Wandelt das Drawable in ein Bitmap um.
     * @param drawable TextDrawable
     * @return Bitmap
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static boolean hasShortcut(Context context, String ID) {
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (shortcutManager != null) {
            List<ShortcutInfo> shortcuts = shortcutManager.getDynamicShortcuts();
            for (ShortcutInfo info : shortcuts) {
                if (info.getId().equals(ID)) {
                    return true;
                }
            }
        }
        return false;
    }
}
