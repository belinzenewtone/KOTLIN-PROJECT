package com.personal.lifeOS

import android.app.Application
import com.personal.lifeOS.bootstrap.BackgroundWorkRegistrar
import com.personal.lifeOS.bootstrap.NotificationBootstrapper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LifeOSApplication : Application() {
    @Inject
    lateinit var notificationBootstrapper: NotificationBootstrapper

    @Inject
    lateinit var backgroundWorkRegistrar: BackgroundWorkRegistrar

    override fun onCreate() {
        super.onCreate()
        notificationBootstrapper.ensureChannels()
        backgroundWorkRegistrar.registerAll()
    }
}
