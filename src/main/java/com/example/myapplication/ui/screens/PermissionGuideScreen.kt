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
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PermissionGuideScreen() {
    val context = LocalContext.current

    var hasOverlay by remember { mutableStateOf(false) }
    var hasAccessibility by remember { mutableStateOf(false) }
    var isIgnoringBattery by remember { mutableStateOf(false) }
    var hasAllFilesAccess by remember { mutableStateOf(false) }

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
            .background(Color(0xFF0C0E17))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val deviceName = com.example.myapplication.util.DeviceInfoHelper.getDeviceBrandAndModel()
                    Text(
                        text = "Android 权限与系统保活指南",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "为了让 $deviceName 上的无障碍拦截和端侧 AI 能够秒级响应且不被系统杀死，请依次配置以下必要权限：",
                        fontSize = 12.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 16.sp
                    )
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
                desc = "用于在拦截瞬间弹出全屏暗色毛玻璃 AI 审批与仪表盘窗口",
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
                desc = "Android 14 限制：需授权此项方可读取 Download 目录下的 GGUF 模型",
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

        // 4. 电池优化白名单 (忽略省电优化)
        item {
            PermissionItem(
                title = "电池无限制 / 忽略省电优化",
                desc = "防止 HyperOS 省电策略在后台休眠时关闭拦截服务",
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

        // 5. HyperOS 自启动与应用详情
        item {
            PermissionItem(
                title = "HyperOS 自启动与后台锁定",
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                            if (isGranted) Color(0x2200E676) else Color(0x2200E5FF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF00E676) else Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCheckable) {
                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "已开启",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "已开启", color = Color(0xFF00E676), fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0D0F18)
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
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
