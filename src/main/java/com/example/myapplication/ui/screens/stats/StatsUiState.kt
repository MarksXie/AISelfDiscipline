package com.example.myapplication.ui.screens.stats

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.myapplication.data.model.AppUsageItem
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.data.model.StatsReport
import com.example.myapplication.data.model.StatusFilter

/**
 * 温润奶咖与陶土色系语义 Token (Warm Light & Terracotta)
 */
object StatsColors {
    val Background = Color(0xFFFAF7F2)          // 燕麦暖白底色
    val Surface = Color(0xFFFFFFFF)             // 纯净暖白卡片
    val SurfaceContainer = Color(0xFFF4EFEB)    // 柔杏奶咖次级容器
    val SurfaceContainerHigh = Color(0xFFEAE2DA)// 强调容器背景
    val Outline = Color(0xFFE8E0D5)             // 浅暖微弱边框 (1dp)
    val OutlineLight = Color(0xFFF0EAE2)        // 极淡分隔线

    val Primary = Color(0xFFC25E3E)             // 陶土暖红棕
    val PrimaryContainer = Color(0xFFFBECE6)    // 浅陶土柔粉
    val OnPrimaryContainer = Color(0xFF7A2E16)  // 深陶土文字

    val Secondary = Color(0xFF8D5B3E)           // 焦糖暖咖
    val SecondaryContainer = Color(0xFFF5ECE4)  // 浅焦糖背景

    val Success = Color(0xFF5C8A60)             // 鼠尾草柔绿
    val SuccessContainer = Color(0xFFEBF3EB)    // 浅鼠尾草背景
    val OnSuccessContainer = Color(0xFF2B522E)  // 深鼠尾草文字

    val Warning = Color(0xFFD9822B)             // 暖琥珀
    val WarningContainer = Color(0xFFFDF3E7)    // 浅暖琥珀背景
    val OnWarningContainer = Color(0xFF78450E)  // 深琥珀文字

    val Danger = Color(0xFFD34545)              // 柔珊瑚红
    val DangerContainer = Color(0xFFFCEAEA)     // 浅珊瑚红背景
    val OnDangerContainer = Color(0xFF751C1C)   // 深珊瑚文字

    val TextPrimary = Color(0xFF2C2420)         // 深暖炭咖 (主标题/正文)
    val TextSecondary = Color(0xFF70645C)       // 暖灰咖 (次要信息)
    val TextMuted = Color(0xFF9E928A)           // 浅咖灰 (时间/提示)
}

/**
 * 指标下钻类型枚举
 */
enum class StatDetailType(val label: String, val color: Color, val containerColor: Color) {
    TOTAL("拦截总数", StatsColors.Primary, StatsColors.PrimaryContainer),
    APPROVED("放行通过", StatsColors.Success, StatsColors.SuccessContainer),
    REJECTED("成功抵制", StatsColors.Danger, StatsColors.DangerContainer)
}

/**
 * 统计页面聚合不可变 UI 状态
 */
@Immutable
data class StatsUiState(
    // 顶部 Tab (0: AI 自律统计报告, 1: 审批记录明细)
    val selectedTabIndex: Int = 0,

    // 周期与时间范围
    val selectedPeriodType: StatsPeriodType = StatsPeriodType.DAY,
    val currentOffset: Int = 0,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val periodLabel: String = "",

    // 客观统计数据
    val totalCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val passRate: Int = 0,
    val totalScreenTimeMs: Long = 0L,
    val topApps: List<AppUsageItem> = emptyList(),
    val hasUsageStatsPermission: Boolean = false,

    // AI 导师评价报告
    val currentReport: StatsReport? = null,
    val isGeneratingReport: Boolean = false,

    // 审批明细与筛选
    val allHistoryRecords: List<ApprovalRecord> = emptyList(),
    val visibleRecords: List<ApprovalRecord> = emptyList(),
    val availableApps: List<String> = emptyList(),
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val selectedAppFilter: String? = null,
    val isAppDropdownExpanded: Boolean = false,
    val clearTimestamp: Long = 0L,

    // 弹窗与下钻状态
    val showConfigDialog: Boolean = false,
    val activeDetailType: StatDetailType? = null,
    val detailDialogRecords: List<ApprovalRecord> = emptyList()
)
