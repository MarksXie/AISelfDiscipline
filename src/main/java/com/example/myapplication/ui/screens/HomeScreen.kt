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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    val blacklistedPackages by repository.blacklistedPackages.collectAsState(initial = emptySet())
    val currentPersona by repository.currentPersona.collectAsState(initial = PersonaType.STRICT_INSTRUCTOR)

    var hasOverlayPermission by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

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
                                    text = if (isAllReady) com.example.myapplication.util.DeviceInfoHelper.getDeviceStatusBanner(context) else "请检查权限配置",
                                    fontSize = 12.sp,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }

                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    repository.setProtectionEnabled(enabled)
                                    if (enabled) {
                                        KeepAliveForegroundService.startService(context)
                                    }
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
                            title = "端侧 Qwen-3B",
                            isOk = true,
                            onClick = onNavigateToAISettings
                        )
                    }
                }
            }
        }

        // 快捷测试拦截卡片
        item {
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
                        Column {
                            Text(
                                text = "审查官：${currentPersona.title}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentPersona.desc,
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE),
                                maxLines = 1
                            )
                        }
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
