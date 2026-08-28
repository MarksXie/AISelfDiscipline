package com.example.myapplication.data.model

import org.json.JSONArray
import org.json.JSONObject

// 统计周期枚举（6个维度）
enum class StatsPeriodType(val id: String, val label: String) {
    DAY("day", "日"),
    WEEK("week", "周"),
    MONTH("month", "月"),
    QUARTER("quarter", "季"),
    HALF_YEAR("half_year", "半年"),
    YEAR("year", "年");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: DAY
    }
}

// 单个应用的使用硬统计项
data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,      // 使用时长 (ms)
    val launchCount: Int = 0    // 打开次数
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("packageName", packageName)
        put("appName", appName)
        put("usageTimeMs", usageTimeMs)
        put("launchCount", launchCount)
    }

    companion object {
        fun fromJson(json: JSONObject) = AppUsageItem(
            packageName = json.optString("packageName"),
            appName = json.optString("appName"),
            usageTimeMs = json.optLong("usageTimeMs"),
            launchCount = json.optInt("launchCount", 0)
        )
    }
}

// 周期硬统计数据（本地准确计算）
data class HardStats(
    val periodStart: Long,
    val periodEnd: Long,
    val totalInterceptions: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val passRate: Int,
    val topAppsUsage: List<AppUsageItem>,
    val totalScreenTimeMs: Long,
    val hasUsageStatsPermission: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("periodStart", periodStart)
        put("periodEnd", periodEnd)
        put("totalInterceptions", totalInterceptions)
        put("approvedCount", approvedCount)
        put("rejectedCount", rejectedCount)
        put("passRate", passRate)
        put("totalScreenTimeMs", totalScreenTimeMs)
        put("hasUsageStatsPermission", hasUsageStatsPermission)
        val arr = JSONArray()
        topAppsUsage.forEach { arr.put(it.toJson()) }
        put("topAppsUsage", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): HardStats {
            val arr = json.optJSONArray("topAppsUsage") ?: JSONArray()
            val apps = mutableListOf<AppUsageItem>()
            for (i in 0 until arr.length()) {
                apps.add(AppUsageItem.fromJson(arr.getJSONObject(i)))
            }
            return HardStats(
                periodStart = json.optLong("periodStart"),
                periodEnd = json.optLong("periodEnd"),
                totalInterceptions = json.optInt("totalInterceptions"),
                approvedCount = json.optInt("approvedCount"),
                rejectedCount = json.optInt("rejectedCount"),
                passRate = json.optInt("passRate"),
                totalScreenTimeMs = json.optLong("totalScreenTimeMs"),
                hasUsageStatsPermission = json.optBoolean("hasUsageStatsPermission", false),
                topAppsUsage = apps
            )
        }
    }
}

// AI 生成的统计报告（持久化存储单元）
data class StatsReport(
    val periodType: String,         // StatsPeriodType.id
    val periodKey: String,          // 唯一键，例如 "day_2026-08-28", "week_2026-W35", "year_2026"
    val periodLabel: String,        // 显示文本，例如 "2026.08.28", "2026 第35周"
    val generatedAt: Long = System.currentTimeMillis(),
    val aiEvaluation: String,       // AI 深度评价与建议
    val hardStats: HardStats,       // 该周期的硬统计指标快照
    val isError: Boolean = false,
    val errorMessage: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("periodType", periodType)
        put("periodKey", periodKey)
        put("periodLabel", periodLabel)
        put("generatedAt", generatedAt)
        put("aiEvaluation", aiEvaluation)
        put("hardStats", hardStats.toJson())
        put("isError", isError)
        put("errorMessage", errorMessage)
    }

    companion object {
        fun fromJson(json: JSONObject) = StatsReport(
            periodType = json.optString("periodType", "day"),
            periodKey = json.optString("periodKey"),
            periodLabel = json.optString("periodLabel"),
            generatedAt = json.optLong("generatedAt", System.currentTimeMillis()),
            aiEvaluation = json.optString("aiEvaluation"),
            hardStats = HardStats.fromJson(json.optJSONObject("hardStats") ?: JSONObject()),
            isError = json.optBoolean("isError", false),
            errorMessage = json.optString("errorMessage", "")
        )
    }
}

// 审批记录筛选器
data class ApprovalFilter(
    val status: StatusFilter = StatusFilter.ALL,
    val appName: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null
)

enum class StatusFilter(val label: String) {
    ALL("全部状态"),
    APPROVED("仅放行通过"),
    REJECTED("仅拦截驳回")
}
