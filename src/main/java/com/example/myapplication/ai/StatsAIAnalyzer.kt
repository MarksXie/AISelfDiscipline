package com.example.myapplication.ai

import android.content.Context
import com.example.myapplication.AppApplication
import com.example.myapplication.data.model.AIEngineType
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSender
import com.example.myapplication.data.model.HardStats
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.data.model.StatsReport
import com.example.myapplication.util.StatsPeriodHelper
import com.example.myapplication.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatsAIAnalyzer {

    private val cloudEngine = CloudOpenAIEngine()
    private val localEngine = LlamaCppEngine()

    /**
     * 生成指定周期的自律统计与评价报告（支持分层级联）
     */
    suspend fun generateReport(
        context: Context,
        periodType: StatsPeriodType,
        offset: Int = 0
    ): StatsReport = withContext(Dispatchers.IO) {
        val repository = AppApplication.instance.repository
        val (startMs, endMs, periodLabel) = StatsPeriodHelper.getPeriodRange(periodType, offset)
        val periodKey = StatsPeriodHelper.getPeriodKey(periodType, startMs)

        // 1. 计算本地硬统计数据
        val allHistory = repository.historyRecords.first()
        val periodHistory = allHistory.filter { it.timestamp in startMs..endMs }
        val totalInterceptions = periodHistory.size
        val approvedCount = periodHistory.count { it.approved }
        val rejectedCount = totalInterceptions - approvedCount
        val passRate = if (totalInterceptions > 0) (approvedCount * 100 / totalInterceptions) else 0

        val hasUsagePermission = UsageStatsHelper.hasUsageStatsPermission(context)
        val appUsages = if (hasUsagePermission) {
            UsageStatsHelper.queryAppUsage(context, startMs, endMs).take(5)
        } else {
            emptyList()
        }
        val totalScreenTimeMs = if (hasUsagePermission) {
            UsageStatsHelper.getTotalScreenTime(context, startMs, endMs)
        } else {
            0L
        }

        val hardStats = HardStats(
            periodStart = startMs,
            periodEnd = endMs,
            totalInterceptions = totalInterceptions,
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            passRate = passRate,
            topAppsUsage = appUsages,
            totalScreenTimeMs = totalScreenTimeMs,
            hasUsageStatsPermission = hasUsagePermission
        )

        // 2. 准备 AI 提示词（分层级联）
        val subKeys = StatsPeriodHelper.getSubPeriodKeys(periodType, startMs, endMs)
        val cachedReportsMap = repository.statsReportsCache.first()
        val subReports = subKeys.mapNotNull { cachedReportsMap[it] }

        val promptContent = buildPromptContent(
            periodType = periodType,
            periodLabel = periodLabel,
            hardStats = hardStats,
            periodHistory = periodHistory,
            subReports = subReports
        )

        // 3. 获取统计专属独立引擎配置
        val statsEngineType = repository.statsEngineType.first()
        val activeEngine: AIEngine = if (statsEngineType == AIEngineType.CLOUD) {
            val cloudCfg = repository.statsActiveCloudConfig.first()
            cloudEngine.apiKey = cloudCfg.apiKey
            cloudEngine.baseUrl = cloudCfg.baseUrl
            cloudEngine.modelName = cloudCfg.modelName
            cloudEngine
        } else {
            localEngine
        }

        // 4. 调用 AI 生成评价
        val systemPrompt = """你是一位洞察敏锐、幽默且富有同理心的「全能自律统计分析导师」。
你的任务是根据用户在指定时间段【${periodLabel}】的手机使用硬统计数据、拦截审批记录以及子周期报告，进行系统性复盘。

【评价要求】：
1. 总体自律定级与生动总结（如：白金自律者/摸鱼潜行者/意志力拉锯中）；
2. 剖析高频使用目的（工作生产力/社交通讯/无目的冲动娱乐）；
3. 提炼自律高光时刻（成功抵制诱惑的代表案例）与薄弱失守环节；
4. 给出下一周期的针对性 2~3 条极简实操自律建议；
5. 口吻幽默风趣、富有启发性，条理清晰，可使用 Markdown 标题和 Emoji 符号排版；
6. 篇幅精炼有力，严格控制在 400 字以内，杜绝冗长套话。
""".trimIndent()

        return@withContext try {
            val evaluationText = if (statsEngineType == AIEngineType.CLOUD) {
                cloudEngine.generateLongReport(
                    userPrompt = promptContent,
                    systemPrompt = systemPrompt
                )
            } else {
                val result = localEngine.evaluateConversation(
                    conversationHistory = listOf(ChatMessage(sender = ChatSender.USER, text = promptContent)),
                    targetAppName = "自律统计报告 ($periodLabel)",
                    systemPrompt = systemPrompt
                )
                if (result.comment.isNotBlank()) result.comment else "本周期自律表现稳健，继续保持！"
            }

            val report = StatsReport(
                periodType = periodType.id,
                periodKey = periodKey,
                periodLabel = periodLabel,
                generatedAt = System.currentTimeMillis(),
                aiEvaluation = evaluationText,
                hardStats = hardStats,
                isError = false
            )

            // 自动存入持久化缓存
            repository.saveStatsReport(report)
            report
        } catch (e: Exception) {
            val errorReport = StatsReport(
                periodType = periodType.id,
                periodKey = periodKey,
                periodLabel = periodLabel,
                generatedAt = System.currentTimeMillis(),
                aiEvaluation = "生成报告失败：${e.localizedMessage ?: "网络或模型连接超时"}",
                hardStats = hardStats,
                isError = true,
                errorMessage = e.localizedMessage ?: "未知错误"
            )
            errorReport
        }
    }

    private fun buildPromptContent(
        periodType: StatsPeriodType,
        periodLabel: String,
        hardStats: HardStats,
        periodHistory: List<ApprovalRecord>,
        subReports: List<StatsReport>
    ): String {
        val sb = StringBuilder()
        sb.append("【报告周期】：$periodLabel (${periodType.label})\n\n")

        // 硬统计部分
        sb.append("【核心客观数据】：\n")
        sb.append("- 拦截总次数：${hardStats.totalInterceptions} 次\n")
        sb.append("- 审批通过放行：${hardStats.approvedCount} 次\n")
        sb.append("- 驳回/放弃拦截：${hardStats.rejectedCount} 次\n")
        sb.append("- 自律过审率：${hardStats.passRate}%\n")
        if (hardStats.hasUsageStatsPermission) {
            val hours = hardStats.totalScreenTimeMs / (1000 * 60 * 60)
            val minutes = (hardStats.totalScreenTimeMs % (1000 * 60 * 60)) / (1000 * 60)
            sb.append("- 屏幕总使用时长：${hours}小时 ${minutes}分钟\n")
            if (hardStats.topAppsUsage.isNotEmpty()) {
                sb.append("- 最常使用应用 TOP5：\n")
                hardStats.topAppsUsage.forEachIndexed { idx, item ->
                    val appMin = item.usageTimeMs / (1000 * 60)
                    sb.append("  ${idx + 1}. ${item.appName}: ${appMin}分钟\n")
                }
            }
        }
        sb.append("\n")

        // 分层级联：如果存在子周期报告（如周报包含日报，年报包含季报/月报）
        if (subReports.isNotEmpty() && periodType != StatsPeriodType.DAY) {
            sb.append("【级联各子周期已生成的 AI 报告摘要】：\n")
            subReports.forEach { sub ->
                val shortSummary = sub.aiEvaluation.lines().take(3).joinToString(" ")
                sb.append("• [${sub.periodLabel}] 过审率${sub.hardStats.passRate}% | 摘要: $shortSummary\n")
            }
            sb.append("\n")
        }

        // 真实审批记录样本（最多取 10 条，避免 Token 溢出）
        if (periodHistory.isNotEmpty()) {
            sb.append("【拦截审批记录样本摘要（共${periodHistory.size}条）】：\n")
            periodHistory.take(10).forEachIndexed { idx, rec ->
                val statusStr = if (rec.approved) "【通过】" else "【驳回/放弃】"
                val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(rec.timestamp))
                sb.append("${idx + 1}. $timeStr ${rec.appName} $statusStr 申请理由:「${rec.reason.take(50)}」 AI评语:「${rec.comment.take(40)}」\n")
            }
        } else {
            sb.append("【审批记录】：本周期内无拦截记录，表现极为克制或暂未触发受保护应用。\n")
        }

        return sb.toString()
    }
}
