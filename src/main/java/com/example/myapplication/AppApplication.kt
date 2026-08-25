package com.example.myapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.myapplication.data.repository.AppLockRepository

class AppApplication : Application() {

    lateinit var repository: AppLockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = AppLockRepository(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI锁机保护前台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持后台拦截服务与状态监听"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ai_guard_service_channel"
        lateinit var instance: AppApplication
            private set
    }
}
