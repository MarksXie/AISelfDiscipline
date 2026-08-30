package com.example.myapplication.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.data.model.PersonaType
import com.example.myapplication.service.KeepAliveForegroundService
import com.example.myapplication.service.OverlayWindowManager
import com.example.myapplication.ui.theme.AmberWarm
import com.example.myapplication.ui.theme.AmberWarmContainer
import com.example.myapplication.ui.theme.CaramelSecondary
import com.example.myapplication.ui.theme.CaramelSecondaryContainer
import com.example.myapplication.ui.theme.CoralDanger
import com.example.myapplication.ui.theme.CoralDangerContainer
import com.example.myapplication.ui.theme.OnAmberWarmContainer
import com.example.myapplication.ui.theme.OnCoralDangerContainer
import com.example.myapplication.ui.theme.OnSageGreenContainer
import com.example.myapplication.ui.theme.OnTerracottaContainer
import com.example.myapplication.ui.theme.SageGreen
import com.example.myapplication.ui.theme.SageGreenContainer
import com.example.myapplication.ui.theme.TerracottaPrimary
import com.example.myapplication.ui.theme.TerracottaPrimaryContainer
import com.example.myapplication.ui.theme.WarmBackground
import com.example.myapplication.ui.theme.WarmBorder
import com.example.myapplication.ui.theme.WarmSurface
import com.example.myapplication.ui.theme.WarmSurfaceContainer
import com.example.myapplication.ui.theme.WarmTextMuted
import com.example.myapplication.ui.theme.WarmTextPrimary
import com.example.myapplication.ui.theme.WarmTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToPermissions: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToAISettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    val isProtectionEnabled by repository.isProtectionEnabled.collectAsState(initial = true)
    val isTestModeEnabled by repository.isTestModeEnabled.collectAsState(initial = false)
    val engineType by repository.engineType.collectAsState(initial = com.example.myapplication.data.model.AIEngineType.CLOUD)
    val blacklistedPackages by repository.blacklistedPackages.collectAsState(initial = emptySet())
    val currentPersona by repository.currentPersona.collectAsState(initial = PersonaType.STRICT_INSTRUCTOR)

    var hasOverlayPermission by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var showDisableProtectionDialog by remember { mutableStateOf(false) }

    if (showDisableProtectionDialog) {
        DisableProtectionDialog(
            onDismiss = { showDisableProtectionDialog = false },
            onConfirmDisable = {
                scope.launch {
                    repository.setProtectionEnabled(false)
                    showDisableProtectionDialog = false
                }
            }
        )
    }

    fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        isAccessibilityEnabled = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            checkPermissions()
            delay(1500)
        }
    }

    val isAllReady = isProtectionEnabled && hasOverlayPermission && isAccessibilityEnabled

    // 呼吸微光动效
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶部 Hero 守护状态复合大卡片（视觉第一焦点）
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color(0x1A8D5B3E)
                    ),
                shape = RoundedCornerShape(22.dp),
                color = WarmSurface,
                border = BorderStroke(
                    1.dp,
                    if (isAllReady) SageGreen.copy(alpha = 0.4f) else AmberWarm.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    WarmSurface,
                                    if (isAllReady) SageGreenContainer.copy(alpha = 0.35f)
                                    else AmberWarmContainer.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 动态呼吸外光环
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        color = if (isAllReady) SageGreen.copy(alpha = pulseAlpha * 0.3f)
                                        else AmberWarm.copy(alpha = pulseAlpha * 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            if (isAllReady) SageGreenContainer else AmberWarmContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAllReady) Icons.Rounded.Shield else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = if (isAllReady) SageGreen else AmberWarm,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (isAllReady) "自律守护系统生效中" else "守护未完全就绪",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAllReady) "核心保活与无障碍已全部就绪" else "缺少必要权限，点击下方检查",
                                    fontSize = 12.sp,
                                    color = WarmTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    scope.launch {
                                        repository.setProtectionEnabled(true)
                                        KeepAliveForegroundService.startService(context)
                                    }
                                } else {
                                    showDisableProtectionDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SageGreen,
                                uncheckedThumbColor = WarmTextSecondary,
                                uncheckedTrackColor = WarmBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 权限状态指标胶囊横向流
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarmSurfaceContainer, RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        HomeStatusIndicator(
                            title = "无障碍拦截",
                            isOk = isAccessibilityEnabled,
                            onClick = onNavigateToPermissions
                        )
                        HomeStatusIndicator(
                            title = "全屏悬浮窗",
                            isOk = hasOverlayPermission,
                            onClick = onNavigateToPermissions
                        )
                        HomeStatusIndicator(
                            title = if (engineType == com.example.myapplication.data.model.AIEngineType.CLOUD) "云端大模型" else "端侧离线引擎",
                            isOk = true,
                            onClick = onNavigateToAISettings
                        )
                    }
                }
            }
        }

        // 2. 两个非对称功能入口胶囊卡片（受保护应用数 + AI 审查官人设）
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 受保护应用卡片
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToBlacklist() },
                    shape = RoundedCornerShape(16.dp),
                    color = WarmSurface,
                    border = BorderStroke(1.dp, WarmBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(TerracottaPrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AppBlocking,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "受保护应用",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                            Text(
                                text = "${blacklistedPackages.size} 个应用",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TerracottaPrimary
                            )
                        }
                    }
                }

                // AI 审查官卡片
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAISettings() },
                    shape = RoundedCornerShape(16.dp),
                    color = WarmSurface,
                    border = BorderStroke(1.dp, WarmBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(CaramelSecondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = CaramelSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI 审查官",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                            Text(
                                text = currentPersona.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CaramelSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. 开发者测试模式总开关与快捷拦截测试卡片
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = WarmSurface,
                border = BorderStroke(1.dp, WarmBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = null,
                                tint = WarmTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "开发者测试模式",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                        }

                        Switch(
                            checked = isTestModeEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    repository.setTestModeEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TerracottaPrimary,
                                uncheckedThumbColor = WarmTextSecondary,
                                uncheckedTrackColor = WarmBorder
                            )
                        )
                    }

                    AnimatedVisibility(visible = isTestModeEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = WarmSurfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "体验 AI 拦截流程",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmTextPrimary
                                        )
                                        Text(
                                            text = "模拟打开受保护应用，测试理由对话与时长拨盘",
                                            fontSize = 11.sp,
                                            color = WarmTextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (!Settings.canDrawOverlays(context)) {
                                                onNavigateToPermissions()
                                            } else {
                                                OverlayWindowManager.show(context, context.packageName, isTest = true)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TerracottaPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("测试拦截", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatusIndicator(
    title: String,
    isOk: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isOk) SageGreen else AmberWarm,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isOk) "就绪" else "待授权",
                fontSize = 11.sp,
                color = if (isOk) SageGreen else AmberWarm,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = WarmTextSecondary
        )
    }
}

/**
 * 关闭自律防护系统的高阻力防冲动确认弹窗：
 * 强制 92 秒冷静倒计时 + 手动输入「我确认关闭自律防护系统」
 */
@Composable
private fun DisableProtectionDialog(
    onDismiss: () -> Unit,
    onConfirmDisable: () -> Unit
) {
    val targetText = "我确认关闭自律防护系统"
    var inputText by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(92) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    val isInputMatched = inputText.trim() == targetText
    val canConfirm = countdown == 0 && isInputMatched

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WarmSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = CoralDanger,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "关闭自律防护系统确认",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmTextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⚠️ 自律防护是保障你专注目标与时间掌控的护城河。为防止一时冲动关闭破戒，请进行冷静思考：",
                    fontSize = 13.sp,
                    color = WarmTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 92s 冷静倒计时卡片
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (countdown > 0) AmberWarmContainer else SageGreenContainer,
                    border = BorderStroke(
                        0.8.dp,
                        if (countdown > 0) AmberWarm.copy(alpha = 0.3f) else SageGreen.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = if (countdown > 0) AmberWarm else SageGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (countdown > 0) "冷静倒计时：${countdown} 秒" else "冷静期已结束，允许确认",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (countdown > 0) OnAmberWarmContainer else OnSageGreenContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "请在下方完整输入确认誓言：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 目标提示词
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = WarmSurfaceContainer
                ) {
                    Text(
                        text = targetText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerracottaPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    enabled = countdown == 0,
                    placeholder = {
                        Text(
                            text = if (countdown > 0) "⏳ 请先等待冷静倒计时归零..." else "请输入「$targetText」",
                            fontSize = 12.sp,
                            color = WarmTextMuted
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isInputMatched) SageGreen else TerracottaPrimary,
                        unfocusedBorderColor = if (isInputMatched) SageGreen else WarmBorder,
                        disabledBorderColor = WarmBorder,
                        focusedTextColor = WarmTextPrimary,
                        unfocusedTextColor = WarmTextPrimary,
                        disabledTextColor = WarmTextMuted,
                        disabledContainerColor = WarmSurfaceContainer
                    )
                )

                if (countdown == 0 && inputText.isNotBlank() && !isInputMatched) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "输入文字与目标誓言不一致",
                        fontSize = 11.sp,
                        color = CoralDanger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDisable,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralDanger,
                    disabledContainerColor = WarmSurfaceContainer,
                    contentColor = Color.White,
                    disabledContentColor = WarmTextMuted
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = when {
                        countdown > 0 -> "冷静期中 (${countdown}s)"
                        !isInputMatched -> "请输入完整语句"
                        else -> "我已充分冷静，确认关闭"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TerracottaPrimary)
            ) {
                Text("保持开启 (推荐)", color = TerracottaPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
