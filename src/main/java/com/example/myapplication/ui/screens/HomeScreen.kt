package com.example.myapplication.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.data.model.PersonaType
import com.example.myapplication.service.AppLockAccessibilityService
import com.example.myapplication.service.KeepAliveForegroundService
import com.example.myapplication.service.OverlayWindowManager
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E17))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 主状态大卡片
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (isAllReady) listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                            else listOf(Color(0xFFFFB300), Color(0xFFFF5252))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isAllReady) Color(0x2200E676) else Color(0x22FF5252),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAllReady) Icons.Rounded.Shield else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = if (isAllReady) Color(0xFF00E676) else Color(0xFFFF5252),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isAllReady) "自律防护系统已生效" else "防护未完全就绪",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAllReady) "核心守护权限已全部就绪" else "缺少必要权限，点击下方检查",
                                    fontSize = 12.sp,
                                    color = Color(0xFF90A4AE)
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
                                    // 阻断直接关闭，弹出 92s 冷静倒计时与文本校验弹窗
                                    showDisableProtectionDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00E676),
                                uncheckedThumbColor = Color(0xFF78909C),
                                uncheckedTrackColor = Color(0xFF263238)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 权限状态指标
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0E111C), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatusIndicator(
                            title = "无障碍拦截",
                            isOk = isAccessibilityEnabled,
                            onClick = onNavigateToPermissions
                        )
                        StatusIndicator(
                            title = "全屏悬浮窗",
                            isOk = hasOverlayPermission,
                            onClick = onNavigateToPermissions
                        )
                        StatusIndicator(
                            title = if (engineType == com.example.myapplication.data.model.AIEngineType.CLOUD) "云端大模型" else "端侧离线引擎",
                            isOk = true,
                            onClick = onNavigateToAISettings
                        )
                    }
                }
            }
        }

        // 开发者测试模式总开关
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "开发者测试模式",
                        fontSize = 12.sp,
                        color = Color(0xFF78909C)
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
                        checkedTrackColor = Color(0xFF00E5FF),
                        uncheckedThumbColor = Color(0xFF546E7A),
                        uncheckedTrackColor = Color(0xFF1B2335)
                    )
                )
            }
        }

        // 快捷测试拦截卡片（受测试模式统一控制显隐）
        item {
            AnimatedVisibility(visible = isTestModeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "测试 AI 理由审批流程",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "立即体验弹窗拦截、AI 评语与转盘时间选择",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE)
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
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0D0F18)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("立即测试", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 受保护黑名单快捷卡片
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBlacklist() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0x2200E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AppBlocking,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "受保护应用 (${blacklistedPackages.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (blacklistedPackages.isEmpty()) "尚未添加应用，点击前往配置" else "已拦截 ${blacklistedPackages.size} 个应用",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                    Text(
                        text = "管理 >",
                        fontSize = 13.sp,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 当前 AI 审查官卡片
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAISettings() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0x22B388FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "审查官：${currentPersona.title}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "调整 >",
                        fontSize = 13.sp,
                        color = Color(0xFFB388FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    title: String,
    isOk: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isOk) Color(0xFF00E676) else Color(0xFFFF5252),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isOk) "正常" else "未开启",
                fontSize = 11.sp,
                color = if (isOk) Color(0xFF00E676) else Color(0xFFFF5252),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFFCFD8DC)
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
        containerColor = Color(0xFF141824),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "关闭自律防护系统确认",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⚠️ 自律防护是保障你专注目标与时间掌控的护城河。为防止一时冲动关闭破戒，请进行冷静思考：",
                    fontSize = 13.sp,
                    color = Color(0xFFCFD8DC),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 92s 冷静倒计时卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (countdown > 0) Color(0x33FFB300) else Color(0x2200E676)
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
                            tint = if (countdown > 0) Color(0xFFFFB300) else Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (countdown > 0) "冷静倒计时：${countdown} 秒" else "冷静期已结束，允许确认",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (countdown > 0) Color(0xFFFFB300) else Color(0xFF00E676)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "请在下方完整输入确认誓言：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90A4AE)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 目标提示词
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E111C), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = targetText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
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
                            color = Color(0xFF546E7A)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isInputMatched) Color(0xFF00E676) else Color(0xFF00E5FF),
                        unfocusedBorderColor = if (isInputMatched) Color(0xFF00E676) else Color(0xFF263238),
                        disabledBorderColor = Color(0xFF1B2335),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color(0xFF546E7A),
                        disabledContainerColor = Color(0xFF0A0C14)
                    )
                )

                if (countdown == 0 && inputText.isNotBlank() && !isInputMatched) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "输入文字与目标誓言不一致",
                        fontSize = 11.sp,
                        color = Color(0xFFFF5252)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDisable,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    disabledContainerColor = Color(0xFF263238),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF78909C)
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
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("保持开启 (推荐)", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
    )
}
