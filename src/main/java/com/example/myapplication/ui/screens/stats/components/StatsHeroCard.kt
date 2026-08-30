package com.example.myapplication.ui.screens.stats.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.screens.stats.StatDetailType
import com.example.myapplication.ui.screens.stats.StatsColors
import com.example.myapplication.ui.screens.stats.StatsUiState

private val TabularNumberStyle = TextStyle(
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.Bold
)

@Composable
fun StatsHeroCard(
    uiState: StatsUiState,
    onOpenDetail: (StatDetailType) -> Unit,
    modifier: Modifier = Modifier
) {
    // 得分计算（有 AI 报告按报告，否则按拦截成功率计算估分）
    val targetScore = uiState.currentReport?.evaluationDetail?.score
        ?: if (uiState.totalCount > 0) (100 - uiState.passRate).coerceIn(0, 100) else 80

    // 动态计数翻滚动画
    val animatedScore by animateIntAsState(
        targetValue = targetScore,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "scoreCounter"
    )

    val animatedTotalCount by animateIntAsState(
        targetValue = uiState.totalCount,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "totalCountCounter"
    )

    val animatedApprovedCount by animateIntAsState(
        targetValue = uiState.approvedCount,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "approvedCountCounter"
    )

    val animatedRejectedCount by animateIntAsState(
        targetValue = uiState.rejectedCount,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "rejectedCountCounter"
    )

    val progressValue by animateFloatAsState(
        targetValue = (animatedScore / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    val (scoreStatusText, scoreThemeColor, scoreBgColor) = when {
        animatedScore >= 80 -> Triple("自律状态卓越 • 专注力极佳", StatsColors.Success, StatsColors.SuccessContainer)
        animatedScore >= 60 -> Triple("表现稳定良好 • 持续保持", StatsColors.Primary, StatsColors.PrimaryContainer)
        else -> Triple("频次偏高 • 建议及时复盘", StatsColors.Danger, StatsColors.DangerContainer)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x1A8D5B3E),
                ambientColor = Color(0x0D8D5B3E)
            ),
        shape = RoundedCornerShape(22.dp),
        color = StatsColors.Surface,
        border = BorderStroke(1.dp, StatsColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StatsColors.Surface,
                            StatsColors.SurfaceContainer.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // 1. 顶部 Hero 仪表盘：环形光晕得分 + 状态定位
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 环形得分环
                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(92.dp),
                        color = StatsColors.SurfaceContainerHigh,
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.size(92.dp),
                        color = scoreThemeColor,
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = Color.Transparent
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animatedScore",
                            fontSize = 32.sp,
                            style = TabularNumberStyle.copy(
                                fontWeight = FontWeight.Black,
                                color = StatsColors.TextPrimary
                            )
                        )
                        Text(
                            text = "综合得分",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatsColors.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 右侧评分定位与周期摘要
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = StatsColors.Primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "AI 自律健康指数",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatsColors.TextSecondary
                        )
                    }

                    // 评分状态气泡
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = scoreBgColor,
                        border = BorderStroke(0.5.dp, scoreThemeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = scoreStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scoreThemeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "基于冲动拦截频次、放行时长与 AI 审查纪要加权评估",
                        fontSize = 11.sp,
                        color = StatsColors.TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. 非对称核心指标矩阵（打破均分网格：主大胶囊 + 次级双胶囊）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 主指标卡片：拦截总数（突出展示）
                Surface(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenDetail(StatDetailType.TOTAL) },
                    shape = RoundedCornerShape(14.dp),
                    color = StatsColors.PrimaryContainer,
                    border = BorderStroke(1.dp, StatsColors.Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "拦截总数",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatsColors.OnPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$animatedTotalCount",
                                fontSize = 24.sp,
                                style = TabularNumberStyle.copy(
                                    fontWeight = FontWeight.Black,
                                    color = StatsColors.Primary
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "查看明细",
                            tint = StatsColors.Primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 次级指标纵向叠放
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 成功抵制
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenDetail(StatDetailType.REJECTED) },
                        shape = RoundedCornerShape(10.dp),
                        color = StatsColors.DangerContainer,
                        border = BorderStroke(0.8.dp, StatsColors.Danger.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "成功抵制",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatsColors.OnDangerContainer
                            )
                            Text(
                                text = "$animatedRejectedCount 次",
                                fontSize = 13.sp,
                                style = TabularNumberStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatsColors.Danger
                                )
                            )
                        }
                    }

                    // 放行通过
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenDetail(StatDetailType.APPROVED) },
                        shape = RoundedCornerShape(10.dp),
                        color = StatsColors.SuccessContainer,
                        border = BorderStroke(0.8.dp, StatsColors.Success.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "放行通过",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatsColors.OnSuccessContainer
                            )
                            Text(
                                text = "$animatedApprovedCount 次",
                                fontSize = 13.sp,
                                style = TabularNumberStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatsColors.Success
                                )
                            )
                        }
                    }
                }
            }

            // 3. 系统屏幕使用时长与耗时 TOP5（温润暖色底条）
            if (uiState.hasUsageStatsPermission) {
                Spacer(modifier = Modifier.height(14.dp))
                val screenHours = uiState.totalScreenTimeMs / (1000 * 60 * 60)
                val screenMins = (uiState.totalScreenTimeMs % (1000 * 60 * 60)) / (1000 * 60)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = StatsColors.SurfaceContainer,
                    border = BorderStroke(0.8.dp, StatsColors.Outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = StatsColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "屏幕总活跃时长：${screenHours} 小时 ${screenMins} 分钟",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatsColors.TextPrimary
                            )
                        }

                        if (uiState.topApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "主要耗时应用 TOP5：",
                                fontSize = 11.sp,
                                color = StatsColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            uiState.topApps.forEachIndexed { idx, item ->
                                val mins = item.usageTimeMs / (1000 * 60)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${item.appName}",
                                        fontSize = 11.sp,
                                        color = StatsColors.TextSecondary
                                    )
                                    Text(
                                        text = "$mins 分钟",
                                        fontSize = 11.sp,
                                        style = TabularNumberStyle.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = StatsColors.Secondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StatsColors.WarningContainer,
                    border = BorderStroke(0.8.dp, StatsColors.Warning.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = StatsColors.Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "未授权使用情况访问权限，统计仅基于拦截记录",
                            fontSize = 11.sp,
                            color = StatsColors.OnWarningContainer
                        )
                    }
                }
            }
        }
    }
}
