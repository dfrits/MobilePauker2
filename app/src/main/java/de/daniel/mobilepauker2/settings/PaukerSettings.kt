package de.daniel.mobilepauker2.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PaukerSettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragmentMain()).commit()
    }
}