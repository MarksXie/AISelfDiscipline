package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.screens.stats.StatisticsViewModel
import com.example.myapplication.ui.screens.stats.StatsColors
import com.example.myapplication.ui.screens.stats.components.StatsAiReviewSection
import com.example.myapplication.ui.screens.stats.components.StatsDetailDialog
import com.example.myapplication.ui.screens.stats.components.StatsHeroCard
import com.example.myapplication.ui.screens.stats.components.StatsHistoryTab
import com.example.myapplication.ui.screens.stats.components.StatsPeriodNavigator
import com.example.myapplication.ui.screens.stats.components.StatsPeriodSelector
import com.example.myapplication.ui.screens.stats.components.StatsTopTabBar

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 进入页面时刷新一次系统使用时间统计
    LaunchedEffect(Unit) {
        viewModel.refreshUsageStats(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StatsColors.Background)
    ) {
        // 1. 顶部 Tab 切换
        StatsTopTabBar(
            selectedTabIndex = uiState.selectedTabIndex,
            onTabSelected = { viewModel.selectTab(it) }
        )

        // 2. Tab 内容过渡切换（弹簧物理动画）
        AnimatedContent(
            targetState = uiState.selectedTabIndex,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "tabContentTransition",
            modifier = Modifier.weight(1f)
        ) { tabIndex ->
            if (tabIndex == 0) {
                // Tab 1: AI 自律统计与复盘报告
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 周期选择 Chip 栏与引擎设置入口
                    item {
                        StatsPeriodSelector(
                            selectedPeriodType = uiState.selectedPeriodType,
                            onSelectPeriodType = { viewModel.selectPeriodType(it) },
                            onOpenConfigDialog = { viewModel.openConfigDialog() }
                        )
                    }

                    // 历史报告翻页控制器
                    item {
                        StatsPeriodNavigator(
                            periodLabel = uiState.periodLabel,
                            currentOffset = uiState.currentOffset,
                            onShiftOffset = { viewModel.shiftOffset(it) }
                        )
                    }

                    // 自律综合指数 Hero 复合大卡片（视觉第一焦点）
                    item {
                        StatsHeroCard(
                            uiState = uiState,
                            onOpenDetail = { viewModel.openDetailDialog(it) }
                        )
                    }

                    // AI 自律复盘导师评价大卡片
                    item {
                        StatsAiReviewSection(
                            uiState = uiState,
                            onGenerateReport = { viewModel.generateReport(context) }
                        )
                    }
                }
            } else {
                // Tab 2: 审批明细流水列表
                StatsHistoryTab(
                    uiState = uiState,
                    onStatusFilterChange = { viewModel.setStatusFilter(it) },
                    onAppFilterChange = { viewModel.setAppFilter(it) },
                    onDropdownExpandChange = { viewModel.setAppDropdownExpanded(it) },
                    onClearHistory = { viewModel.clearHistoryDisplay() },
                    onResetHistory = { viewModel.resetHistoryDisplay() }
                )
            }
        }
    }

    // 3. 指标下钻明细弹窗
    if (uiState.activeDetailType != null) {
        StatsDetailDialog(
            detailType = uiState.activeDetailType!!,
            periodLabel = uiState.periodLabel,
            records = uiState.detailDialogRecords,
            onDismiss = { viewModel.closeDetailDialog() }
        )
    }

    // 4. 统计 AI 引擎配置弹窗
    if (uiState.showConfigDialog) {
        StatsEngineConfigDialog(
            onDismiss = { viewModel.closeConfigDialog() }
        )
    }
}
