package de.daniel.mobilepauker2.application

import android.app.Application

class PaukerApplication : Application() {
    lateinit var applicationSingletonComponent: ApplicationSingletonComponent

    override fun onCreate() {
        super.onCreate()

        applicationSingletonComponent = DaggerApplicationSingletonComponent.builder()
            .providerModule(ProviderModule(this))
            .build()
    }
}