package com.daniel.mobilepauker2.core

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.di.appModule
import com.daniel.mobilepauker2.main.MainMenu
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

open class PaukerApplication : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidLogger()
            androidContext(baseContext)
            modules(appModule)
        }

        startActivity(Intent(this, MainMenu::class.java))
        finish()
    }
}