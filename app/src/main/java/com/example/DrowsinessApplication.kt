package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.DriverRepository

class DrowsinessApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var repository: DriverRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        userPreferences = UserPreferences(this)
        repository = DriverRepository(database.drowsinessEventDao(), userPreferences)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_MONITORING_SERVICE,
                "Driver Monitoring Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for active drowsiness monitoring"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_DROWSINESS_ALERT,
                "Critical Drowsiness Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority warnings when driver drowsiness is detected"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_MONITORING_SERVICE = "driver_monitoring_channel"
        const val CHANNEL_DROWSINESS_ALERT = "driver_alert_channel"

        lateinit var instance: DrowsinessApplication
            private set
    }
}
