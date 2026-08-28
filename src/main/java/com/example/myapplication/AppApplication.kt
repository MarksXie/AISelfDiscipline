package com.example.myapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.data.repository.AppLockRepository
import com.example.myapplication.service.StatsReportWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AppApplication : Application() {

    lateinit var repository: AppLockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = AppLockRepository(this)
        createNotificationChannel()
        scheduleStatsReportWorkers()
    }

    private fun scheduleStatsReportWorkers() {
        try {
            val workManager = WorkManager.getInstance(this)

            // 计算距离今晚 23:00 的毫秒延迟
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (now.after(target)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelayMs = target.timeInMillis - now.timeInMillis

            // 日报：每 24 小时执行一次，初始延迟至当晚 23:00
            val dailyRequest = PeriodicWorkRequestBuilder<StatsReportWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("periodType" to StatsPeriodType.DAY.id))
                .build()

            workManager.enqueueUniquePeriodicWork(
                "stats_daily_report_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyRequest
            )

            // 周报：每 7 天执行一次
            val weeklyRequest = PeriodicWorkRequestBuilder<StatsReportWorker>(
                7, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("periodType" to StatsPeriodType.WEEK.id))
                .build()

            workManager.enqueueUniquePeriodicWork(
                "stats_weekly_report_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                weeklyRequest
            )
        } catch (e: Exception) {
            // WorkManager 初始化异常捕获
        }
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
