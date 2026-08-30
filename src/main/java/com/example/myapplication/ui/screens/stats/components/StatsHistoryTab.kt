package com.example.myapplication.ui.screens.stats.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.StatusFilter
import com.example.myapplication.ui.screens.stats.StatsColors
import com.example.myapplication.ui.screens.stats.StatsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsHistoryTab(
    uiState: StatsUiState,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onAppFilterChange: (String?) -> Unit,
    onDropdownExpandChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onResetHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 筛选条件栏
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 1.5.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0x148D5B3E)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = StatsColors.Surface,
                border = BorderStroke(1.dp, StatsColors.Outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = StatsColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "记录筛选",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsColors.TextPrimary
                            )
                        }

                        // App 筛选下拉菜单
                        Box {
                            TextButton(onClick = { onDropdownExpandChange(true) }) {
                                Text(
                                    text = uiState.selectedAppFilter ?: "全部应用 ▼",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatsColors.Primary
                                )
                            }
                            DropdownMenu(
                                expanded = uiState.isAppDropdownExpanded,
                                onDismissRequest = { onDropdownExpandChange(false) },
                                modifier = Modifier.background(StatsColors.Surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部应用", color = StatsColors.TextPrimary) },
                                    onClick = { onAppFilterChange(null) }
                                )
                                uiState.availableApps.forEach { appName ->
                                    DropdownMenuItem(
                                        text = { Text(appName, color = StatsColors.TextPrimary) },
                                        onClick = { onAppFilterChange(appName) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 状态筛选 Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusFilter.entries.forEach { filter ->
                            val isSelected = uiState.statusFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStatusFilterChange(filter) },
                                label = { Text(filter.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = StatsColors.Outline,
                                    selectedBorderColor = StatsColors.Primary,
                                    borderWidth = 1.dp
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatsColors.Primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = StatsColors.SurfaceContainer,
                                    labelColor = StatsColors.TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. 清理与条数提示栏
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "展示明细 (${uiState.visibleRecords.size} 条)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatsColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row {
                    if (uiState.clearTimestamp > 0L) {
                        TextButton(onClick = onResetHistory) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                tint = StatsColors.Success,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("恢复全部", fontSize = 12.sp, color = StatsColors.Success)
                        }
                    }

                    if (uiState.visibleRecords.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "清理记录",
                                tint = StatsColors.TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清理历史", fontSize = 12.sp, color = StatsColors.TextMuted)
                        }
                    }
                }
            }
        }

        // 3. 流水列表或空状态
        if (uiState.visibleRecords.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = StatsColors.SurfaceContainer
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.clearTimestamp > 0L) "历史记录已隐藏，数据完好保留供统计使用" else "暂无符合条件的审批记录",
                            fontSize = 13.sp,
                            color = StatsColors.TextMuted
                        )
                    }
                }
            }
        } else {
            items(uiState.visibleRecords, key = { it.id }) { record ->
                HistoryItemCard(record = record)
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: ApprovalRecord, modifier: Modifier = Modifier) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(record.timestamp))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x108D5B3E)
            ),
        shape = RoundedCornerShape(14.dp),
        color = StatsColors.Surface,
        border = BorderStroke(1.dp, StatsColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 头部：状态图标 + 应用名 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusBg = if (record.approved) StatsColors.SuccessContainer else StatsColors.DangerContainer
                    val statusTint = if (record.approved) StatsColors.Success else StatsColors.Danger

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(statusBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (record.approved) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatsColors.TextPrimary
                    )
                }

                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = StatsColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 理由
            if (record.reason.isNotBlank()) {
                val isMultiTurn = record.reason.contains("➔")
                if (isMultiTurn) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = StatsColors.SurfaceContainer,
                        border = BorderStroke(0.6.dp, StatsColors.Outline)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "多轮自律问答纪要",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsColors.Primary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = record.reason,
                                fontSize = 11.sp,
                                color = StatsColors.TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "申请理由：${record.reason}",
                        fontSize = 12.sp,
                        color = StatsColors.TextSecondary,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // AI 评语
            Text(
                text = "AI 评语：${record.comment}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (record.approved) StatsColors.Success else StatsColors.Danger,
                lineHeight = 16.sp
            )

            // 放行时长
            if (record.approved && record.allowedMinutes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = StatsColors.Secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "放行时长：${record.allowedMinutes} 分钟",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatsColors.Secondary
                    )
                }
            }
        }
    }
}
