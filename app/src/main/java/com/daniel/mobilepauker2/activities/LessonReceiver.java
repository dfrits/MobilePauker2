package com.daniel.mobilepauker2.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.SyncDialog;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LessonReceiver extends Activity {
    private final Activity context = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);
        ((TextView) findViewById(R.id.pTitle)).setText(R.string.import_title);

        Intent intent = getIntent();
        Log.d("LessonReceiver::importLesson", "ENTRY");
        if (intent == null || intent.getData() == null) {
            Log.d("LessonReceiver::importLesson filename: ", "intent is null");
            PaukerManager.showToast(context, "Keine Paukerdatei", Toast.LENGTH_LONG);
            finish();
        } else {
            Uri fileUri = intent.getData();
            String filePath = fileUri.getEncodedPath();
            Log.d("LessonReceiver::importLesson filePath: ", filePath);

            if (filePath == null || PaukerManager.instance().isNameEmpty(filePath)) {
                PaukerManager.showToast(context, "Keine Datei gefunden", Toast.LENGTH_SHORT);
                finish();
            } else {
                File localFile = new File(Environment.getExternalStorageDirectory()
                        + PaukerManager.instance().getApplicationDataDirectory(),
                        new File(filePath).getName());

                if (localFile.exists()) {
                    Log.d("LessonReceiver::importLesson localFile: ", "File existiert bereits");
                    PaukerManager.showToast(context, "Datei existiert bereits", Toast.LENGTH_LONG);
                    finish();
                } else {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(fileUri);
                        FileOutputStream outputStream = new FileOutputStream(localFile);
                        if (inputStream != null) {
                            copyFile(inputStream, outputStream);
                            Log.d("LessonReceiver::importLesson", "import success");

                            ModelManager.instance().addLesson(context, localFile);
                            Log.d("LessonReceiver::importLesson", "lesson added");
                            PaukerManager.instance().loadLessonFromFile(localFile);
                            Log.d("LessonReceiver::importLesson", "lesson opend");

                            Log.d("LessonReceiver::importLesson", "start MainMenu");
                            PaukerManager.showToast(context, R.string.lesson_import_success, Toast.LENGTH_LONG);

                            if (SettingsManager.instance().getBoolPreference(context, SettingsManager.Keys.AUTO_SYNC)) {
                                uploadFile(localFile);
                                Log.d("LessonReceiver::importLesson", "Lesson uploaded");
                            } else {
                                restartApp();
                            }
                        }
                    } catch (IOException e) {
                        PaukerManager.showToast(context, "Fehler beim einlesen", Toast.LENGTH_SHORT);
                        finish();
                    }
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            restartApp();
        }
    }

    private void restartApp() {
        Log.d("LessonReceiver::restartApp", "App restarted");
        Intent mainMenu = new Intent(context, MainMenu.class);
        mainMenu.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainMenu);
        finish();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void uploadFile(File localFile) {
        String accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.DROPBOX_ACCESS_TOKEN, null);
        Intent intent = new Intent(context, SyncDialog.class);
        intent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken);
        intent.putExtra(SyncDialog.FILES, localFile);
        startActivityForResult(intent, Constants.REQUEST_CODE_SYNC_DIALOG);
    }
}
