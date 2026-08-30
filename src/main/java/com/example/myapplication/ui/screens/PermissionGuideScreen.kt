package com.example.myapplication.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AmberWarm
import com.example.myapplication.ui.theme.AmberWarmContainer
import com.example.myapplication.ui.theme.CaramelSecondary
import com.example.myapplication.ui.theme.CaramelSecondaryContainer
import com.example.myapplication.ui.theme.OnSageGreenContainer
import com.example.myapplication.ui.theme.SageGreen
import com.example.myapplication.ui.theme.SageGreenContainer
import com.example.myapplication.ui.theme.TerracottaPrimary
import com.example.myapplication.ui.theme.TerracottaPrimaryContainer
import com.example.myapplication.ui.theme.WarmBackground
import com.example.myapplication.ui.theme.WarmBorder
import com.example.myapplication.ui.theme.WarmSurface
import com.example.myapplication.ui.theme.WarmTextMuted
import com.example.myapplication.ui.theme.WarmTextPrimary
import com.example.myapplication.ui.theme.WarmTextSecondary
import com.example.myapplication.util.UsageStatsHelper
import kotlinx.coroutines.delay

@Composable
fun PermissionGuideScreen() {
    val context = LocalContext.current

    var hasOverlay by remember { mutableStateOf(false) }
    var hasAccessibility by remember { mutableStateOf(false) }
    var isIgnoringBattery by remember { mutableStateOf(false) }
    var hasAllFilesAccess by remember { mutableStateOf(false) }
    var hasUsageStats by remember { mutableStateOf(false) }

    fun refreshStatus() {
        hasOverlay = Settings.canDrawOverlays(context)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        hasAccessibility = list.any { it.resolveInfo.serviceInfo.packageName == context.packageName }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isIgnoringBattery = pm.isIgnoringBatteryOptimizations(context.packageName)

        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

        hasUsageStats = UsageStatsHelper.hasUsageStatsPermission(context)
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshStatus()
            delay(1200)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color(0x108D5B3E)
                    ),
                shape = RoundedCornerShape(18.dp),
                color = WarmSurface,
                border = BorderStroke(1.dp, WarmBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(TerracottaPrimaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val deviceName = com.example.myapplication.util.DeviceInfoHelper.getDeviceBrandAndModel()
                        Text(
                            text = "系统保活与权限授权清单",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "保障在 $deviceName 上无障碍拦截与端侧 AI 秒级响应，防止后台被系统省电杀除",
                            fontSize = 11.sp,
                            color = WarmTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 1. 无障碍服务
        item {
            PermissionItem(
                title = "无障碍拦截服务 (必需)",
                desc = "用于实时感知目标受保护 App 的启动并执行拦截压后台",
                icon = Icons.Rounded.Layers,
                isGranted = hasAccessibility,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 2. 全屏悬浮窗权限
        item {
            PermissionItem(
                title = "显示在其他应用上层 (必需)",
                desc = "用于在拦截瞬间弹出毛玻璃 AI 审批与时长选择窗口",
                icon = Icons.Rounded.Widgets,
                isGranted = hasOverlay,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 3. 所有文件访问权限 (端侧模型读取)
        item {
            PermissionItem(
                title = "所有文件访问权限 (模型读取必需)",
                desc = "Android 14+ 需授权此项方可读取 Download 目录下的 GGUF 模型",
                icon = Icons.Rounded.Folder,
                isGranted = hasAllFilesAccess,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 4. 使用情况访问权限 (自律统计必需)
        item {
            PermissionItem(
                title = "使用情况访问权限 (自律统计必需)",
                desc = "精确获取各 App 真实使用时长，为统计报表提供系统数据",
                icon = Icons.Rounded.Analytics,
                isGranted = hasUsageStats,
                onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 5. 电池优化白名单 (忽略省电优化)
        item {
            PermissionItem(
                title = "电池无限制 / 忽略省电优化",
                desc = "防止厂商省电策略在后台休眠时关闭常驻拦截服务",
                icon = Icons.Rounded.BatterySaver,
                isGranted = isIgnoringBattery,
                onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 6. HyperOS 自启动与应用详情
        item {
            PermissionItem(
                title = "厂商系统自启动与后台锁定",
                desc = "建议开启【允许自启动】并在多任务列表中给本应用【加锁】以获最强保活",
                icon = Icons.Rounded.PowerSettingsNew,
                isGranted = false,
                isCheckable = false,
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    desc: String,
    icon: ImageVector,
    isGranted: Boolean,
    isCheckable: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = WarmSurface,
        border = BorderStroke(
            1.dp,
            if (isGranted) SageGreen.copy(alpha = 0.35f) else WarmBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isGranted) SageGreenContainer else TerracottaPrimaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) SageGreen else TerracottaPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = WarmTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCheckable) {
                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SageGreenContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "已开启",
                                tint = SageGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "已就绪", color = OnSageGreenContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("去授权", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "前往设置",
                    tint = CaramelSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
