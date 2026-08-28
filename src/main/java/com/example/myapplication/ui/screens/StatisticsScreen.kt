package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.ai.StatsAIAnalyzer
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.data.model.StatsReport
import com.example.myapplication.data.model.StatusFilter
import com.example.myapplication.util.StatsPeriodHelper
import com.example.myapplication.util.UsageStatsHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen() {
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("AI 自律统计报告", "审批记录明细")

    var showConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E17))
    ) {
        // 顶部二级导航 Tab
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF10131E),
            contentColor = Color(0xFF00E5FF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF00E5FF),
                    height = 3.dp
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                        )
                    }
                )
            }
        }

        if (selectedTabIndex == 0) {
            StatsReportTabContent(
                onOpenConfigDialog = { showConfigDialog = true }
            )
        } else {
            ApprovalRecordsTabContent()
        }
    }

    if (showConfigDialog) {
        StatsEngineConfigDialog(
            onDismiss = { showConfigDialog = false }
        )
    }
}

/**
 * Tab 1: AI 自律统计报告内容区
 */
@Composable
private fun StatsReportTabContent(
    onOpenConfigDialog: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    var selectedPeriodType by remember { mutableStateOf(StatsPeriodType.DAY) }
    var currentOffset by remember { mutableIntStateOf(0) } // 0=当前周期，-1=上一周期

    val (startMs, endMs, periodLabel) = remember(selectedPeriodType, currentOffset) {
        StatsPeriodHelper.getPeriodRange(selectedPeriodType, currentOffset)
    }
    val currentPeriodKey = remember(selectedPeriodType, startMs) {
        StatsPeriodHelper.getPeriodKey(selectedPeriodType, startMs)
    }

    val cachedReportsMap by repository.statsReportsCache.collectAsState(initial = emptyMap())
    val currentReport = cachedReportsMap[currentPeriodKey]

    var isGenerating by remember { mutableStateOf(false) }
    var activeDetailType by remember { mutableStateOf<StatDetailType?>(null) }

    // 实时计算当前选择区间的硬统计
    val allHistory by repository.historyRecords.collectAsState(initial = emptyList())
    val periodHistory = remember(allHistory, startMs, endMs) {
        allHistory.filter { it.timestamp in startMs..endMs }
    }
    val totalCount = periodHistory.size
    val approvedCount = periodHistory.count { it.approved }
    val rejectedCount = totalCount - approvedCount
    val passRate = if (totalCount > 0) (approvedCount * 100 / totalCount) else 0

    val hasUsageStats = remember { UsageStatsHelper.hasUsageStatsPermission(context) }
    val topApps = remember(startMs, endMs, hasUsageStats) {
        if (hasUsageStats) UsageStatsHelper.queryAppUsage(context, startMs, endMs).take(5) else emptyList()
    }
    val totalScreenTimeMs = remember(startMs, endMs, hasUsageStats) {
        if (hasUsageStats) UsageStatsHelper.getTotalScreenTime(context, startMs, endMs) else 0L
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 周期选择与引擎设置栏
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 横向滚动周期 Chip (日/周/月/季/半年/年)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatsPeriodType.entries.forEach { pType ->
                        val isSelected = selectedPeriodType == pType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPeriodType = pType
                                currentOffset = 0 // 切换周期时重置到最新一期
                            },
                            label = { Text(pType.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color(0xFF0D0F18),
                                containerColor = Color(0xFF141824),
                                labelColor = Color(0xFF90A4AE)
                            )
                        )
                    }
                }

                // 右上角统计专属 AI 引擎设置入口
                IconButton(
                    onClick = onOpenConfigDialog,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "统计引擎设置",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. 左右历史报告翻页导航栏
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentOffset -= 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "上一周期",
                            tint = Color(0xFF00E5FF)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = periodLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (currentOffset == 0) "当前进行中" else "历史往期报告",
                            fontSize = 11.sp,
                            color = if (currentOffset == 0) Color(0xFF00E676) else Color(0xFF90A4AE)
                        )
                    }

                    IconButton(
                        onClick = { if (currentOffset < 0) currentOffset += 1 },
                        enabled = currentOffset < 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "下一周期",
                            tint = if (currentOffset < 0) Color(0xFF00E5FF) else Color(0xFF37474F)
                        )
                    }
                }
            }
        }

        // 3. 客观硬统计卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "客观使用与拦截指标",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            title = "拦截总数",
                            value = "$totalCount",
                            color = Color(0xFF00E5FF),
                            onClick = { activeDetailType = StatDetailType.TOTAL }
                        )
                        StatItem(
                            title = "放行通过",
                            value = "$approvedCount",
                            color = Color(0xFF00E676),
                            onClick = { activeDetailType = StatDetailType.APPROVED }
                        )
                        StatItem(
                            title = "成功抵制",
                            value = "$rejectedCount",
                            color = Color(0xFFFF5252),
                            onClick = { activeDetailType = StatDetailType.REJECTED }
                        )
                        StatItem(title = "自律过审率", value = "$passRate%", color = Color(0xFFB388FF))
                    }

                    if (hasUsageStats) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val screenHours = totalScreenTimeMs / (1000 * 60 * 60)
                        val screenMins = (totalScreenTimeMs % (1000 * 60 * 60)) / (1000 * 60)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E111C), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "系统总屏幕时长：${screenHours}小时 ${screenMins}分钟",
                                fontSize = 12.sp,
                                color = Color(0xFFECEFF1),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (topApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "主要耗时应用 TOP5：",
                                fontSize = 11.sp,
                                color = Color(0xFF90A4AE)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            topApps.forEachIndexed { idx, item ->
                                val mins = item.usageTimeMs / (1000 * 60)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${item.appName}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCFD8DC)
                                    )
                                    Text(
                                        text = "${mins} 分钟",
                                        fontSize = 12.sp,
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x22FFB300), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "未授权使用情况访问权限，统计仅基于拦截记录",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        }

        // 4. AI 自律导师评价报告大卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0x2200E5FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI 自律复盘导师评价",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isGenerating = true
                                    StatsAIAnalyzer.generateReport(
                                        context = context,
                                        periodType = selectedPeriodType,
                                        offset = currentOffset
                                    )
                                    isGenerating = false
                                }
                            },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0D0F18)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("分析中...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentReport == null) "生成报告" else "重新生成",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentReport != null) {
                        val detail = currentReport.evaluationDetail
                        val isStructured = detail.summary.isNotEmpty() || detail.good.isNotEmpty() || detail.problem.isNotEmpty() || detail.suggestion.isNotEmpty()

                        if (isStructured) {
                            // 1. 评分与总体评价栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0E111C), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            when {
                                                detail.score >= 80 -> Color(0x3300E676)
                                                detail.score >= 60 -> Color(0x3300E5FF)
                                                else -> Color(0x33FF5252)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${detail.score}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            detail.score >= 80 -> Color(0xFF00E676)
                                            detail.score >= 60 -> Color(0xFF00E5FF)
                                            else -> Color(0xFFFF5252)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "自律得分：${detail.score}分 • 综合复盘",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF90A4AE)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        detail.summary.forEachIndexed { index, sumItem ->
                                            val cleanText = sumItem
                                                .replaceFirst(Regex("^[0-9]+[.\\u3001\\s、]+"), "")
                                                .replaceFirst(Regex("^[-*•]\\s*"), "")
                                                .trim()
                                            Row(verticalAlignment = Alignment.Top) {
                                                Text(
                                                    text = "•",
                                                    color = Color(0xFF00E5FF),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cleanText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White,
                                                    lineHeight = 17.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. 执行亮点 (分点)
                            BulletedSectionCard(
                                icon = "🌟",
                                title = "执行亮点",
                                titleColor = Color(0xFF00E676),
                                bgColor = Color(0x1500E676),
                                bulletColor = Color(0xFF00E676),
                                items = detail.good
                            )

                            if (detail.good.isNotEmpty() && (detail.problem.isNotEmpty() || detail.suggestion.isNotEmpty())) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 3. 关注问题 (分点)
                            BulletedSectionCard(
                                icon = "⚠️",
                                title = "值得关注",
                                titleColor = Color(0xFFFFB300),
                                bgColor = Color(0x15FFB300),
                                bulletColor = Color(0xFFFFB300),
                                items = detail.problem
                            )

                            if (detail.problem.isNotEmpty() && detail.suggestion.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 4. 改进建议 (分点)
                            BulletedSectionCard(
                                icon = "💡",
                                title = "改进建议",
                                titleColor = Color(0xFF00E5FF),
                                bgColor = Color(0x1500E5FF),
                                bulletColor = Color(0xFF00E5FF),
                                items = detail.suggestion
                            )
                        } else {
                            Text(
                                text = currentReport.aiEvaluation,
                                fontSize = 13.sp,
                                color = Color(0xFFECEFF1),
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val genTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(currentReport.generatedAt))
                        Text(
                            text = "报告生成时间：$genTimeStr",
                            fontSize = 10.sp,
                            color = Color(0xFF78909C)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E111C), RoundedCornerShape(12.dp))
                                .padding(vertical = 28.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.HourglassEmpty,
                                    contentDescription = null,
                                    tint = Color(0xFF546E7A),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "本周期暂无自动缓存报告",
                                    fontSize = 13.sp,
                                    color = Color(0xFF78909C)
                                )
                                Text(
                                    text = "点击右上角「生成报告」即可由 AI 进行全维度评价",
                                    fontSize = 11.sp,
                                    color = Color(0xFF546E7A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeDetailType != null) {
        val detailType = activeDetailType!!
        val filteredRecords = remember(periodHistory, detailType) {
            when (detailType) {
                StatDetailType.TOTAL -> periodHistory
                StatDetailType.APPROVED -> periodHistory.filter { it.approved }
                StatDetailType.REJECTED -> periodHistory.filter { !it.approved }
            }
        }

        StatDetailDialog(
            detailType = detailType,
            periodLabel = periodLabel,
            records = filteredRecords,
            onDismiss = { activeDetailType = null }
        )
    }
}

/**
 * Tab 2: 审批记录明细内容区（支持筛选与安全清理）
 */
@Composable
private fun ApprovalRecordsTabContent() {
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    val allHistory by repository.historyRecords.collectAsState(initial = emptyList())
    val clearTimestamp by repository.historyClearTimestamp.collectAsState(initial = 0L)

    // 筛选条件状态
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var selectedAppFilter by remember { mutableStateOf<String?>(null) } // null = 全部应用
    var isAppDropdownExpanded by remember { mutableStateOf(false) }

    // 所有出现过的不同 App 列表供下拉选择
    val availableApps = remember(allHistory) {
        allHistory.map { it.appName }.distinct().sorted()
    }

    // 过滤逻辑：1. 清理时间戳过滤；2. 状态过滤；3. App 过滤
    val visibleRecords = remember(allHistory, clearTimestamp, statusFilter, selectedAppFilter) {
        allHistory.filter { record ->
            val afterClear = record.timestamp >= clearTimestamp
            val matchStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.APPROVED -> record.approved
                StatusFilter.REJECTED -> !record.approved
            }
            val matchApp = selectedAppFilter == null || record.appName == selectedAppFilter
            afterClear && matchStatus && matchApp
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 筛选条件栏
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "记录筛选",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // App 筛选下拉菜单
                        Box {
                            TextButton(onClick = { isAppDropdownExpanded = true }) {
                                Text(
                                    text = selectedAppFilter ?: "全部应用 ▼",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            DropdownMenu(
                                expanded = isAppDropdownExpanded,
                                onDismissRequest = { isAppDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部应用") },
                                    onClick = {
                                        selectedAppFilter = null
                                        isAppDropdownExpanded = false
                                    }
                                )
                                availableApps.forEach { appName ->
                                    DropdownMenuItem(
                                        text = { Text(appName) },
                                        onClick = {
                                            selectedAppFilter = appName
                                            isAppDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 状态筛选 Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusFilter.entries.forEach { filter ->
                            val isSelected = statusFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { statusFilter = filter },
                                label = { Text(filter.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E5FF),
                                    selectedLabelColor = Color(0xFF0D0F18),
                                    containerColor = Color(0xFF0E111C),
                                    labelColor = Color(0xFF90A4AE)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. 清理与显示全部状态栏
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "展示明细 (${visibleRecords.size} 条)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90CAF9),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row {
                    if (clearTimestamp > 0L) {
                        TextButton(
                            onClick = { scope.launch { repository.resetHistoryDisplay() } }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("恢复显示全部", fontSize = 12.sp, color = Color(0xFF00E676))
                        }
                    }

                    if (visibleRecords.isNotEmpty()) {
                        TextButton(
                            onClick = { scope.launch { repository.clearHistoryDisplay() } }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "清理前端",
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清理历史", fontSize = 12.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }
            }
        }

        // 3. 流水列表或空状态
        if (visibleRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (clearTimestamp > 0L) "历史记录已隐藏，数据完好保留供统计使用" else "暂无符合筛选条件的审批记录",
                            fontSize = 13.sp,
                            color = Color(0xFF546E7A)
                        )
                    }
                }
            }
        } else {
            items(visibleRecords, key = { it.id }) { record ->
                HistoryItemCard(record = record)
            }
        }
    }
}

enum class StatDetailType(val label: String, val color: Color) {
    TOTAL("拦截总数", Color(0xFF00E5FF)),
    APPROVED("放行通过", Color(0xFF00E676)),
    REJECTED("成功抵制", Color(0xFFFF5252))
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                }
            )
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFF90A4AE))
            if (onClick != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF546E7A),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun StatDetailDialog(
    detailType: StatDetailType,
    periodLabel: String,
    records: List<ApprovalRecord>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = detailType.color,
                    contentColor = Color(0xFF0D0F18)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("关闭", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(detailType.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${detailType.label}明细 (${records.size}条)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "统计周期：$periodLabel",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color(0xFF0E111C), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassEmpty,
                                contentDescription = null,
                                tint = Color(0xFF546E7A),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "该周期内暂无【${detailType.label}】记录",
                                fontSize = 12.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(records, key = { it.id }) { record ->
                            HistoryItemCard(record = record)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color(0xFF141824)
    )
}

@Composable
private fun HistoryItemCard(record: ApprovalRecord) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (record.approved) Color(0x2200E676) else Color(0x22FF5252),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (record.approved) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                            contentDescription = null,
                            tint = if (record.approved) Color(0xFF00E676) else Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = Color(0xFF78909C)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (record.reason.isNotBlank()) {
                val isMultiTurn = record.reason.contains("➔")
                if (isMultiTurn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F121E), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "多轮自律问答纪要",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = record.reason,
                                fontSize = 11.sp,
                                color = Color(0xFFCFD8DC),
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "申请理由：${record.reason}",
                        fontSize = 12.sp,
                        color = Color(0xFFECEFF1),
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = "AI 评语：${record.comment}",
                fontSize = 12.sp,
                color = if (record.approved) Color(0xFF81C784) else Color(0xFFEF9A9A),
                lineHeight = 16.sp
            )

            if (record.approved && record.allowedMinutes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "放行时长：${record.allowedMinutes} 分钟",
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletedSectionCard(
    icon: String,
    title: String,
    titleColor: Color,
    bgColor: Color,
    bulletColor: Color,
    items: List<String>
) {
    if (items.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { index, itemText ->
                    val cleanText = itemText
                        .replaceFirst(Regex("^[0-9]+[.\\u3001\\s、]+"), "")
                        .replaceFirst(Regex("^[-*•]\\s*"), "")
                        .trim()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                                .background(bulletColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = bulletColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cleanText,
                            fontSize = 12.sp,
                            color = Color(0xFFECEFF1),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
