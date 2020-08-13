package com.daniel.mobilepauker2.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

open class PaukerApplication : AppCompatActivity() {
    private var koinStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!koinStarted) {
            startKoin {
                androidLogger()
                androidContext(baseContext)
                modules(appModule)
            }
            koinStarted = true
        }
    }
}