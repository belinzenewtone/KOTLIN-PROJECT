package com.personal.lifeOS

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.personal.lifeOS.bootstrap.BackgroundWorkRegistrar
import com.personal.lifeOS.bootstrap.NotificationBootstrapper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LifeOSApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationBootstrapper: NotificationBootstrapper

    @Inject
    lateinit var backgroundWorkRegistrar: BackgroundWorkRegistrar

    override fun onCreate() {
        super.onCreate()
        notificationBootstrapper.ensureChannels()
        backgroundWorkRegistrar.registerAll()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
