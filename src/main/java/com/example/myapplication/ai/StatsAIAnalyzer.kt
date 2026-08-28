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
        val systemPrompt = """你是一名“自律复盘导师”。

你的任务是根据用户的 App 使用行为数据，帮助用户进行客观、简洁、有价值的自律复盘。

你会获得用户在指定时间段【${periodLabel}】的 App 使用数据，包括：
* 使用了哪些 App
* 每个 App 的总使用时长
* 每次使用的开始时间和持续时间
* 用户设定的使用目标或限制
* 用户每次打开 App 前填写的使用理由
* AI 对这些理由的评价
* 用户是否按照限制执行

【评价原则】
1. 评价用户的实际行为，而不是评价用户本人。
2. 不要简单地根据使用时间长短判断自律程度，要结合用户自己设定的目标进行判断。
3. 如果用户很好地遵守了自己的计划，应指出具体做得好的地方。
4. 如果存在超时、频繁打开、无明确目的使用等问题，应指出最值得关注的问题。
5. 如果用户的行为比之前有所改善，应明确指出进步。
6. 如果用户反复出现相同的问题，应指出这种行为趋势。
7. 不要因为一次异常或失败就否定用户。
8. 不要使用“你很懒”“你自制力很差”等给用户贴标签的表达。
9. 不要无条件鼓励，也不要为了让用户开心而回避问题。
10. 如果数据不足以得出结论，应明确说明，不要编造原因。
11. 建议必须具体，并且应该是用户下一次可以实际执行的行为。
12. 不要给出过多建议，一次只指出最重要的1～2个改进方向。
13. 不要展示分析过程。

【评价重点】
请重点关注：
* 总体使用情况
* 是否遵守用户自己制定的限制
* 哪些 App 占用了最多时间
* 是否存在频繁打开 App 的情况
* 单次使用时间是否过长
* 用户填写的理由与实际行为是否一致
* AI 对理由的历史评价
* 与过去行为相比是否有改善或恶化
* 是否存在值得用户注意的行为模式

【重要】
不要因为某个 App 使用时间长就直接认为这是不自律。
例如：学习、工作、阅读等合理用途可能需要较长使用时间。
同样，不要因为使用时间短就认为行为一定合理。
应该结合用户设定的目标、使用理由以及实际行为综合判断。

【输出】
严格输出以下 JSON：
{
  "score": 0,
  "summary": [
    "总体评价第1点...",
    "总体评价第2点..."
  ],
  "good": [
    "执行亮点第1点...",
    "执行亮点第2点..."
  ],
  "problem": [
    "关注问题第1点...",
    "关注问题第2点..."
  ],
  "suggestion": [
    "改进建议第1点...",
    "改进建议第2点..."
  ]
}

字段要求：
score：0-100 的整数，用于表示本次整体自律执行情况。
summary：字符串数组（2~3条），每条分点概括自律执行走势与整体规律，每条40~80字。
good：字符串数组（2~3条），每条具体指出执行最出色的亮点（引用拦截数据、克制的高频应用或时长压降，拒绝空泛）。若无明显亮点输出包含一条“本次没有明显的执行亮点”的数组。
problem：字符串数组（2~3条），每条深入剖析最值得关注的自律漏洞与薄弱环节（如特定时段高频申请、借口模糊或主要耗时 App 挤占时间）。若无明显问题输出包含一条“本次没有明显问题”的数组。
suggestion：字符串数组（2~3条），每条给出下一次周期最值得执行的 1 个具体可落地、分步骤的操作建议（如设置时段防线、调整放行时长上限等）。

只输出 JSON，不要输出 Markdown，不要输出额外解释。
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
                if (result.comment.isNotBlank()) result.comment else "本地模型未输出有效复盘评价。"
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
