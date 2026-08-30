package com.example.myapplication.ui.screens.stats.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.ui.screens.stats.StatsColors
import com.example.myapplication.ui.screens.stats.StatsUiState

@Composable
fun StatsTopTabBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf("AI 自律复盘报告", "审批记录明细")

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = StatsColors.Surface,
        contentColor = StatsColors.Primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = StatsColors.Primary,
                height = 3.dp
            )
        },
        divider = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StatsColors.OutlineLight,
                content = {}
            )
        },
        modifier = modifier
    ) {
        tabTitles.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) StatsColors.Primary else StatsColors.TextSecondary
                    )
                }
            )
        }
    }
}

@Composable
fun StatsPeriodSelector(
    selectedPeriodType: StatsPeriodType,
    onSelectPeriodType: (StatsPeriodType) -> Unit,
    onOpenConfigDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 横向滚动周期 Chip (日 / 周 / 月 / 季 / 半年 / 年)
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
                    onClick = { onSelectPeriodType(pType) },
                    label = {
                        Text(
                            text = pType.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
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
                        containerColor = StatsColors.Surface,
                        labelColor = StatsColors.TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧设置按钮
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = StatsColors.Surface,
            border = BorderStroke(1.dp, StatsColors.Outline)
        ) {
            IconButton(
                onClick = onOpenConfigDialog,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "统计引擎设置",
                    tint = StatsColors.Secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatsPeriodNavigator(
    periodLabel: String,
    currentOffset: Int,
    onShiftOffset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.5.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x148D5B3E)
            ),
        shape = RoundedCornerShape(14.dp),
        color = StatsColors.Surface,
        border = BorderStroke(1.dp, StatsColors.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onShiftOffset(-1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "上一周期",
                    tint = StatsColors.Primary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = periodLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatsColors.TextPrimary
                )
                Text(
                    text = if (currentOffset == 0) "当前进行中" else "往期历史报告",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (currentOffset == 0) StatsColors.Success else StatsColors.TextMuted
                )
            }

            IconButton(
                onClick = { onShiftOffset(1) },
                enabled = currentOffset < 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "下一周期",
                    tint = if (currentOffset < 0) StatsColors.Primary else StatsColors.TextMuted.copy(alpha = 0.4f)
                )
            }
        }
    }
}
