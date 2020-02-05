package com.daniel.mobilepauker2.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.SettingsFragment

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
            .replace(R.id.content, SettingsFragment()).commit()
    }
}