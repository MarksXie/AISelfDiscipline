package com.example.myapplication.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.example.myapplication.data.model.AppUsageItem

object UsageStatsHelper {

    /**
     * 检查是否已授予 PACKAGE_USAGE_STATS 权限
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * 查询指定时间区间内各个 App 的实际使用统计，并按使用时长降序排列
     */
    fun queryAppUsage(context: Context, startTime: Long, endTime: Long): List<AppUsageItem> {
        if (!hasUsageStatsPermission(context) || startTime >= endTime) {
            return emptyList()
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val pm = context.packageManager

        return try {
            val statsList = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            ) ?: return emptyList()

            val usageMap = mutableMapOf<String, Long>()
            statsList.forEach { stats ->
                val time = stats.totalTimeInForeground
                if (time > 0) {
                    usageMap[stats.packageName] = (usageMap[stats.packageName] ?: 0L) + time
                }
            }

            val ownPackage = context.packageName

            usageMap.filter { (pkg, time) -> pkg != ownPackage && time >= 10_000L } // 过滤掉小于10秒的碎片应用
                .map { (pkg, totalTime) ->
                    val appName = try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkg
                    }
                    AppUsageItem(
                        packageName = pkg,
                        appName = appName,
                        usageTimeMs = totalTime
                    )
                }
                .sortedByDescending { it.usageTimeMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 计算指定时间区间内的总屏幕前台使用时长（ms）
     */
    fun getTotalScreenTime(context: Context, startTime: Long, endTime: Long): Long {
        val appUsages = queryAppUsage(context, startTime, endTime)
        return appUsages.sumOf { it.usageTimeMs }
    }
}
