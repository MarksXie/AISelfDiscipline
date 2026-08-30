package com.example.myapplication.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AmberWarm
import com.example.myapplication.ui.theme.CaramelSecondary
import com.example.myapplication.ui.theme.SageGreen
import com.example.myapplication.ui.theme.SageGreenContainer
import com.example.myapplication.ui.theme.TerracottaPrimary
import com.example.myapplication.ui.theme.TerracottaPrimaryContainer
import com.example.myapplication.ui.theme.WarmBorder
import com.example.myapplication.ui.theme.WarmSurface
import com.example.myapplication.ui.theme.WarmSurfaceContainer
import com.example.myapplication.ui.theme.WarmTextMuted
import com.example.myapplication.ui.theme.WarmTextPrimary
import com.example.myapplication.ui.theme.WarmTextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class PresetDuration(
    val minutes: Int,
    val label: String,
    val isMeeting: Boolean = false,
    val isAi: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationDialPicker(
    modifier: Modifier = Modifier,
    initialMinutes: Int = 15,
    aiSuggestedMinutes: Int? = null,
    onConfirm: (minutes: Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }
    var isDragging by remember { mutableStateOf(false) }

    // 预设时长：涵盖 AI 建议、短时专注、常规事务、长程会议（最高 8 小时 / 480 分钟）
    val presetDurations = remember(aiSuggestedMinutes) {
        val list = mutableListOf<PresetDuration>()
        if (aiSuggestedMinutes != null && aiSuggestedMinutes > 0) {
            val label = if (aiSuggestedMinutes >= 60 && aiSuggestedMinutes % 60 == 0) {
                "✨ AI建议 ${aiSuggestedMinutes / 60}小时"
            } else {
                "✨ AI建议 ${aiSuggestedMinutes}分钟"
            }
            list.add(PresetDuration(aiSuggestedMinutes, label, isAi = true))
        }
        val defaultPresets = listOf(
            PresetDuration(15, "15分钟"),
            PresetDuration(30, "30分钟"),
            PresetDuration(45, "45分钟"),
            PresetDuration(60, "1小时"),
            PresetDuration(90, "1.5小时"),
            PresetDuration(120, "2小时 (开会)", isMeeting = true),
            PresetDuration(180, "3小时 (长会)", isMeeting = true),
            PresetDuration(240, "4小时 (半天)", isMeeting = true),
            PresetDuration(480, "8小时 (全天)", isMeeting = true)
        )
        defaultPresets.forEach { item ->
            if (list.none { it.minutes == item.minutes }) {
                list.add(item)
            }
        }
        list
    }

    // 分段双精度映射算法
    fun minutesToAngle(minutes: Int): Float {
        return if (minutes <= 60) {
            (minutes.toFloat() / 60f) * 180f
        } else {
            180f + ((minutes - 60).toFloat() / 420f) * 180f
        }.coerceIn(1f, 360f)
    }

    fun angleToMinutes(angle: Float): Int {
        return if (angle <= 180f) {
            val raw = (angle / 180f) * 60f
            if (raw <= 15f) raw.roundToInt().coerceIn(1, 60)
            else ((raw / 5f).roundToInt() * 5).coerceIn(5, 60)
        } else {
            val raw = 60f + ((angle - 180f) / 180f) * 420f
            ((raw / 15f).roundToInt() * 15).coerceIn(60, 480)
        }
    }

    val rawSweepAngle = minutesToAngle(selectedMinutes)
    val animatedSweepAngle by animateFloatAsState(
        targetValue = rawSweepAngle,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "sweepAngle"
    )
    val sweepAngle = if (isDragging) rawSweepAngle else animatedSweepAngle

    fun updateMinutesFromOffset(offset: Offset, sizeWidth: Float, sizeHeight: Float) {
        val center = Offset(sizeWidth / 2f, sizeHeight / 2f)
        val touchVec = offset - center
        var angle = (atan2(touchVec.y, touchVec.x) * (180f / PI.toFloat()) + 90f)
        if (angle < 0) angle += 360f
        selectedMinutes = angleToMinutes(angle)
    }

    // 格式化主展示文本
    val hours = selectedMinutes / 60
    val remainingMins = selectedMinutes % 60
    val formattedDurationText = when {
        hours > 0 && remainingMins > 0 -> "${hours}小时 ${remainingMins}分"
        hours > 0 -> "${hours} 小时"
        else -> "${remainingMins} 分钟"
    }

    val stepSize = if (selectedMinutes <= 60) 5 else 15

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = TerracottaPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "设定放行时长 (支持长程场景)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WarmTextPrimary
                )
            )
        }

        // 步进器独立放置在转盘两边
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：智能减少按钮
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(WarmSurfaceContainer)
                    .clickable {
                        selectedMinutes = (selectedMinutes - stepSize).coerceAtLeast(1)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "减少",
                        tint = WarmTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "-${stepSize}m",
                        fontSize = 9.sp,
                        color = WarmTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 中间：温润奶咖转盘主体与中心时间文本
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .padding(2.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(185.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isDragging = true
                                    updateMinutesFromOffset(offset, size.width.toFloat(), size.height.toFloat())
                                    tryAwaitRelease()
                                    isDragging = false
                                },
                                onTap = { offset ->
                                    updateMinutesFromOffset(offset, size.width.toFloat(), size.height.toFloat())
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    updateMinutesFromOffset(offset, size.width.toFloat(), size.height.toFloat())
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    updateMinutesFromOffset(change.position, size.width.toFloat(), size.height.toFloat())
                                }
                            )
                        }
                ) {
                    val strokeWidth = 13.dp.toPx()
                    val radius = (size.minDimension - strokeWidth - 14.dp.toPx()) / 2f
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)

                    // 绘制表盘刻度
                    val tickCount = 24
                    for (i in 0 until tickCount) {
                        val tickAngle = (i * (360f / tickCount) - 90f) * (PI.toFloat() / 180f)
                        val isMajor = i % 2 == 0
                        val isSpecial = i == 0 || i == 12
                        val tickLength = if (isSpecial) 9.dp.toPx() else if (isMajor) 6.dp.toPx() else 3.dp.toPx()
                        val outerR = radius + strokeWidth / 2f + 3.dp.toPx()
                        val innerR = outerR + tickLength

                        val p1 = Offset(
                            centerOffset.x + outerR * cos(tickAngle),
                            centerOffset.y + outerR * sin(tickAngle)
                        )
                        val p2 = Offset(
                            centerOffset.x + innerR * cos(tickAngle),
                            centerOffset.y + innerR * sin(tickAngle)
                        )
                        drawLine(
                            color = if (isSpecial) TerracottaPrimary else if (isMajor) CaramelSecondary.copy(alpha = 0.5f) else WarmBorder,
                            start = p1,
                            end = p2,
                            strokeWidth = if (isSpecial) 2.dp.toPx() else if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // 底环 (柔杏奶咖色)
                    drawArc(
                        color = Color(0xFFEDE5DC),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 陶土焦糖柔和高亮弧线
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                TerracottaPrimary,
                                CaramelSecondary,
                                AmberWarm,
                                SageGreen,
                                TerracottaPrimary
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle.coerceIn(1f, 360f),
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 指针圆点
                    val angleRad = (-90f + sweepAngle) * (PI.toFloat() / 180f)
                    val handleCenter = Offset(
                        x = centerOffset.x + radius * cos(angleRad),
                        y = centerOffset.y + radius * sin(angleRad)
                    )
                    drawCircle(
                        color = TerracottaPrimary.copy(alpha = 0.25f),
                        radius = strokeWidth * 0.9f,
                        center = handleCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = strokeWidth * 0.65f,
                        center = handleCenter
                    )
                    drawCircle(
                        color = TerracottaPrimary,
                        radius = strokeWidth * 0.35f,
                        center = handleCenter
                    )
                }

                // 中心时间展示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = formattedDurationText,
                        fontSize = if (hours > 0 && remainingMins > 0) 20.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (hours >= 2) CaramelSecondary else TerracottaPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hours >= 1) "总计 $selectedMinutes 分钟" else "专注倒计时",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmTextSecondary
                    )
                }
            }

            // 右侧：智能增加按钮
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(WarmSurfaceContainer)
                    .clickable {
                        selectedMinutes = (selectedMinutes + stepSize).coerceAtMost(480)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "增加",
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "+${stepSize}m",
                        fontSize = 9.sp,
                        color = TerracottaPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 预设快捷标签
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = 3
        ) {
            presetDurations.forEach { preset ->
                val isSelected = selectedMinutes == preset.minutes
                val bgColor by animateColorAsState(
                    if (isSelected) {
                        when {
                            preset.isAi -> TerracottaPrimary
                            preset.isMeeting -> CaramelSecondary
                            else -> SageGreen
                        }
                    } else if (preset.isAi) {
                        TerracottaPrimaryContainer
                    } else {
                        WarmSurfaceContainer
                    },
                    label = "chipBg"
                )
                val textColor = if (isSelected) Color.White else if (preset.isAi) TerracottaPrimary else WarmTextPrimary

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { selectedMinutes = preset.minutes }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (preset.isMeeting) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = preset.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 确认放行按钮
        Button(
            onClick = { onConfirm(selectedMinutes) },
            colors = ButtonDefaults.buttonColors(
                containerColor = SageGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "确认并进入应用 (放行 $formattedDurationText)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
