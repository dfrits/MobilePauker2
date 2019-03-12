package com.daniel.mobilepauker2.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.TextView;

import com.daniel.mobilepauker2.BuildConfig;
import com.daniel.mobilepauker2.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class About extends AppCompatActivity {
    private static Map<Locale, String> fileNameMap;
    static
    {
        fileNameMap = new HashMap<>();
        fileNameMap.put(Locale.ENGLISH, "file:///android_asset/instructions_en.html");
        fileNameMap.put(Locale.GERMANY, "file:///android_asset/instructions_de.html");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        WebView webView = findViewById(R.id.tAboutText);
        String fileName = fileNameMap.get(Locale.getDefault());
        fileName = fileName == null ? fileNameMap.get(Locale.ENGLISH) : fileName;
        webView.loadUrl(fileName);
        webView.setBackgroundColor(getColor(R.color.defaultBackground));
        String version = "Version: "+BuildConfig.VERSION_NAME;
        ((TextView)findViewById(R.id.tVersion)).setText(version);
    }
}
