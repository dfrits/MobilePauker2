package com.daniel.mobilepauker2.dropbox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;
import com.dropbox.core.android.Auth;

public class DropboxAccDialog extends Activity {
    public static String AUTH_MODE = "AUTH_MODE";
    public static String UNL_MODE = "UNL_MODE";
    private SharedPreferences prefs;
    private boolean assStarted = false;
    private boolean firstStart = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);

        RelativeLayout progressBar = findViewById(R.id.pFrame);
        progressBar.setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.pTitle);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = prefs.getString(Constants.DROPBOX_ACCESS_TOKEN, null);

        Intent intent = getIntent();
        if (intent.getBooleanExtra(AUTH_MODE, false)) {
            title.setText(R.string.association);
            if (accessToken == null) {
                Auth.startOAuth2Authentication(this, Constants.DROPBOX_APP_KEY);
                assStarted = true;
            } else {
                PaukerManager.showToast(this, "Bereits verbunden", Toast.LENGTH_SHORT);
                setResult(RESULT_CANCELED);
                finish();
            }
        } else if (intent.getBooleanExtra(UNL_MODE, false)) {
            title.setText(R.string.unlinking);
            if (accessToken == null) {
                PaukerManager.showToast(this, "Nicht möglich", Toast.LENGTH_SHORT);
                setResult(RESULT_CANCELED);
                finish();
            } else {
                prefs.edit().remove(Constants.DROPBOX_ACCESS_TOKEN).apply();
                PaukerManager.showToast(this, "Dropbox getrennt", Toast.LENGTH_SHORT);
                Log.d("SettingsFragment::initSyncPrefs", "accessTocken = null");
                setResult(RESULT_OK);
                finish();
            }
        } else finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!firstStart && assStarted) {
            String accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply();
                PaukerManager.showToast(this, "Verbunden", Toast.LENGTH_SHORT);
                setResult(RESULT_OK);
            } else {
                PaukerManager.showToast(this, "Fehler beim Verbinden", Toast.LENGTH_SHORT);
                setResult(RESULT_CANCELED);
            }
            finish();
        }
        firstStart = false;
    }
}
