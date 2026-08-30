package com.example.myapplication.ui.screens.stats.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.screens.stats.StatsColors
import com.example.myapplication.ui.screens.stats.StatsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsAiReviewSection(
    uiState: StatsUiState,
    onGenerateReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = uiState.currentReport
    val isGenerating = uiState.isGeneratingReport

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x148D5B3E)
            ),
        shape = RoundedCornerShape(20.dp),
        color = StatsColors.Surface,
        border = BorderStroke(1.dp, StatsColors.Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. 顶部标题与操作按键
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(StatsColors.PrimaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = StatsColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 自律复盘导师评价",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatsColors.TextPrimary
                    )
                }

                Button(
                    onClick = onGenerateReport,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatsColors.Primary,
                        contentColor = Color.White,
                        disabledContainerColor = StatsColors.SurfaceContainerHigh,
                        disabledContentColor = StatsColors.TextMuted
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = StatsColors.Primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("分析中...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (report == null) "生成报告" else "重新复盘",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 报告主体或空状态
            if (report != null) {
                val detail = report.evaluationDetail
                val isStructured = detail.summary.isNotEmpty() || detail.good.isNotEmpty() ||
                        detail.problem.isNotEmpty() || detail.suggestion.isNotEmpty()

                if (isStructured) {
                    // 总体评分与要点纪要
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = StatsColors.SurfaceContainer,
                        border = BorderStroke(0.8.dp, StatsColors.Outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            val badgeColor = when {
                                detail.score >= 80 -> StatsColors.Success
                                detail.score >= 60 -> StatsColors.Primary
                                else -> StatsColors.Danger
                            }
                            val badgeBg = when {
                                detail.score >= 80 -> StatsColors.SuccessContainer
                                detail.score >= 60 -> StatsColors.PrimaryContainer
                                else -> StatsColors.DangerContainer
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(badgeBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${detail.score}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeColor
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "自律得分：${detail.score} 分 • 导师综合评述",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatsColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    detail.summary.forEach { sumItem ->
                                        val cleanText = sumItem
                                            .replaceFirst(Regex("^[0-9]+[.\\u3001\\s、]+"), "")
                                            .replaceFirst(Regex("^[-*•]\\s*"), "")
                                            .trim()
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "•",
                                                color = StatsColors.Primary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = cleanText,
                                                fontSize = 12.sp,
                                                color = StatsColors.TextSecondary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 执行亮点 (鼠尾草绿)
                    ReviewBulletSection(
                        icon = "🌟",
                        title = "执行亮点",
                        titleColor = StatsColors.Success,
                        bgColor = StatsColors.SuccessContainer,
                        bulletColor = StatsColors.Success,
                        items = detail.good
                    )

                    if (detail.good.isNotEmpty() && (detail.problem.isNotEmpty() || detail.suggestion.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 关注问题 (暖琥珀)
                    ReviewBulletSection(
                        icon = "⚠️",
                        title = "值得关注",
                        titleColor = StatsColors.Warning,
                        bgColor = StatsColors.WarningContainer,
                        bulletColor = StatsColors.Warning,
                        items = detail.problem
                    )

                    if (detail.problem.isNotEmpty() && detail.suggestion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 改进建议 (陶土红棕)
                    ReviewBulletSection(
                        icon = "💡",
                        title = "改进建议",
                        titleColor = StatsColors.Primary,
                        bgColor = StatsColors.PrimaryContainer,
                        bulletColor = StatsColors.Primary,
                        items = detail.suggestion
                    )
                } else {
                    // 非结构化纯文本
                    Text(
                        text = report.aiEvaluation,
                        fontSize = 13.sp,
                        color = StatsColors.TextPrimary,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                val genTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(report.generatedAt))
                Text(
                    text = "报告生成时间：$genTimeStr",
                    fontSize = 10.sp,
                    color = StatsColors.TextMuted
                )
            } else {
                // 空状态
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = StatsColors.SurfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassEmpty,
                            contentDescription = null,
                            tint = StatsColors.TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "本周期暂无自动缓存复盘报告",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatsColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击右上角「生成报告」即可由 AI 进行全维度自律复盘",
                            fontSize = 11.sp,
                            color = StatsColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewBulletSection(
    icon: String,
    title: String,
    titleColor: Color,
    bgColor: Color,
    bulletColor: Color,
    items: List<String>
) {
    if (items.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(0.6.dp, bulletColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                                color = StatsColors.TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
