package com.systemtools.v4

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CH_SERVICE,
                "Layanan sistem",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_TRIGGER,
                "Pintasan folder",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val CH_SERVICE = "sys40_service"
        const val CH_TRIGGER = "sys40_trigger"
    }
}
