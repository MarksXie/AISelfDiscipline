package com.example.myapplication.ui.screens.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AppApplication
import com.example.myapplication.ai.StatsAIAnalyzer
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.data.model.StatusFilter
import com.example.myapplication.data.repository.AppLockRepository
import com.example.myapplication.util.StatsPeriodHelper
import com.example.myapplication.util.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val repository: AppLockRepository = AppApplication.instance.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        // 监听并组合底层数据流
        viewModelScope.launch {
            combine(
                repository.historyRecords,
                repository.statsReportsCache,
                repository.historyClearTimestamp
            ) { history, cachedReports, clearTs ->
                Triple(history, cachedReports, clearTs)
            }.collect { (history, cachedReports, clearTs) ->
                recalculateState(history, cachedReports, clearTs)
            }
        }
        updatePeriodCalculation()
    }

    private fun updatePeriodCalculation() {
        val currentPeriodType = _uiState.value.selectedPeriodType
        val currentOffset = _uiState.value.currentOffset

        val (startMs, endMs, periodLabel) = StatsPeriodHelper.getPeriodRange(currentPeriodType, currentOffset)
        _uiState.update { state ->
            state.copy(
                startMs = startMs,
                endMs = endMs,
                periodLabel = periodLabel
            )
        }
    }

    private fun recalculateState(
        history: List<ApprovalRecord>,
        cachedReports: Map<String, com.example.myapplication.data.model.StatsReport>,
        clearTs: Long
    ) {
        val currentState = _uiState.value
        val startMs = currentState.startMs
        val endMs = currentState.endMs
        val currentPeriodType = currentState.selectedPeriodType

        // 1. 本周期历史记录与指标
        val periodHistory = history.filter { it.timestamp in startMs..endMs }
        val totalCount = periodHistory.size
        val approvedCount = periodHistory.count { it.approved }
        val rejectedCount = totalCount - approvedCount
        val passRate = if (totalCount > 0) (approvedCount * 100 / totalCount) else 0

        // 2. 当前周期缓存报告
        val currentPeriodKey = StatsPeriodHelper.getPeriodKey(currentPeriodType, startMs)
        val currentReport = cachedReports[currentPeriodKey]

        // 3. 所有出现过的 App 列表
        val availableApps = history.map { it.appName }.distinct().sorted()

        // 4. 过滤后的明细流水
        val statusFilter = currentState.statusFilter
        val selectedAppFilter = currentState.selectedAppFilter
        val visibleRecords = history.filter { record ->
            val afterClear = record.timestamp >= clearTs
            val matchStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.APPROVED -> record.approved
                StatusFilter.REJECTED -> !record.approved
            }
            val matchApp = selectedAppFilter == null || record.appName == selectedAppFilter
            afterClear && matchStatus && matchApp
        }

        // 5. 下钻弹窗记录同步
        val detailDialogRecords = currentState.activeDetailType?.let { detailType ->
            when (detailType) {
                StatDetailType.TOTAL -> periodHistory
                StatDetailType.APPROVED -> periodHistory.filter { it.approved }
                StatDetailType.REJECTED -> periodHistory.filter { !it.approved }
            }
        } ?: emptyList()

        _uiState.update { state ->
            state.copy(
                allHistoryRecords = history,
                clearTimestamp = clearTs,
                totalCount = totalCount,
                approvedCount = approvedCount,
                rejectedCount = rejectedCount,
                passRate = passRate,
                currentReport = currentReport,
                availableApps = availableApps,
                visibleRecords = visibleRecords,
                detailDialogRecords = detailDialogRecords
            )
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTabIndex = tabIndex) }
    }

    fun selectPeriodType(periodType: StatsPeriodType) {
        _uiState.update { it.copy(selectedPeriodType = periodType, currentOffset = 0) }
        updatePeriodCalculation()
        triggerRefresh()
    }

    fun shiftOffset(delta: Int) {
        val newOffset = _uiState.value.currentOffset + delta
        if (newOffset <= 0) {
            _uiState.update { it.copy(currentOffset = newOffset) }
            updatePeriodCalculation()
            triggerRefresh()
        }
    }

    fun refreshUsageStats(context: Context) {
        val hasPermission = UsageStatsHelper.hasUsageStatsPermission(context)
        val startMs = _uiState.value.startMs
        val endMs = _uiState.value.endMs

        val topApps = if (hasPermission) {
            UsageStatsHelper.queryAppUsage(context, startMs, endMs).take(5)
        } else {
            emptyList()
        }

        val totalScreenTimeMs = if (hasPermission) {
            UsageStatsHelper.getTotalScreenTime(context, startMs, endMs)
        } else {
            0L
        }

        _uiState.update { state ->
            state.copy(
                hasUsageStatsPermission = hasPermission,
                topApps = topApps,
                totalScreenTimeMs = totalScreenTimeMs
            )
        }
    }

    fun generateReport(context: Context) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingReport = true) }
            StatsAIAnalyzer.generateReport(
                context = context,
                periodType = state.selectedPeriodType,
                offset = state.currentOffset
            )
            _uiState.update { it.copy(isGeneratingReport = false) }
        }
    }

    fun openDetailDialog(type: StatDetailType) {
        val state = _uiState.value
        val periodHistory = state.allHistoryRecords.filter { it.timestamp in state.startMs..state.endMs }
        val filtered = when (type) {
            StatDetailType.TOTAL -> periodHistory
            StatDetailType.APPROVED -> periodHistory.filter { it.approved }
            StatDetailType.REJECTED -> periodHistory.filter { !it.approved }
        }
        _uiState.update { it.copy(activeDetailType = type, detailDialogRecords = filtered) }
    }

    fun closeDetailDialog() {
        _uiState.update { it.copy(activeDetailType = null, detailDialogRecords = emptyList()) }
    }

    fun setStatusFilter(filter: StatusFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
        triggerRefresh()
    }

    fun setAppFilter(appName: String?) {
        _uiState.update { it.copy(selectedAppFilter = appName, isAppDropdownExpanded = false) }
        triggerRefresh()
    }

    fun setAppDropdownExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isAppDropdownExpanded = expanded) }
    }

    fun clearHistoryDisplay() {
        viewModelScope.launch {
            repository.clearHistoryDisplay()
        }
    }

    fun resetHistoryDisplay() {
        viewModelScope.launch {
            repository.resetHistoryDisplay()
        }
    }

    fun openConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = true) }
    }

    fun closeConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false) }
    }

    private fun triggerRefresh() {
        val state = _uiState.value
        val history = state.allHistoryRecords
        val clearTs = state.clearTimestamp

        // 重新过滤 visibleRecords
        val visibleRecords = history.filter { record ->
            val afterClear = record.timestamp >= clearTs
            val matchStatus = when (state.statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.APPROVED -> record.approved
                StatusFilter.REJECTED -> !record.approved
            }
            val matchApp = state.selectedAppFilter == null || record.appName == state.selectedAppFilter
            afterClear && matchStatus && matchApp
        }

        val periodHistory = history.filter { it.timestamp in state.startMs..state.endMs }
        val totalCount = periodHistory.size
        val approvedCount = periodHistory.count { it.approved }
        val rejectedCount = totalCount - approvedCount
        val passRate = if (totalCount > 0) (approvedCount * 100 / totalCount) else 0

        _uiState.update {
            it.copy(
                visibleRecords = visibleRecords,
                totalCount = totalCount,
                approvedCount = approvedCount,
                rejectedCount = rejectedCount,
                passRate = passRate
            )
        }
    }
}
