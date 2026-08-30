package com.example.myapplication

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.service.KeepAliveForegroundService
import com.example.myapplication.ui.screens.AISettingsScreen
import com.example.myapplication.ui.screens.BlacklistScreen
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.PermissionGuideScreen
import com.example.myapplication.ui.screens.StatisticsScreen
import com.example.myapplication.ui.theme.CaramelSecondary
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.SageGreen
import com.example.myapplication.ui.theme.TerracottaPrimary
import com.example.myapplication.ui.theme.TerracottaPrimaryContainer
import com.example.myapplication.ui.theme.WarmBackground
import com.example.myapplication.ui.theme.WarmBorder
import com.example.myapplication.ui.theme.WarmBorderLight
import com.example.myapplication.ui.theme.WarmSurface
import com.example.myapplication.ui.theme.WarmSurfaceContainer
import com.example.myapplication.ui.theme.WarmTextMuted
import com.example.myapplication.ui.theme.WarmTextPrimary
import com.example.myapplication.ui.theme.WarmTextSecondary

enum class MainDestination(val label: String) {
    HOME("首页守护"),
    BLACKLIST("受保护应用"),
    AI_SETTINGS("AI审查官"),
    PERMISSIONS("权限保活"),
    HISTORY("自律统计");

    val icon: ImageVector
        get() = when (this) {
            HOME -> Icons.Rounded.Shield
            BLACKLIST -> Icons.Rounded.AppBlocking
            AI_SETTINGS -> Icons.Rounded.Psychology
            PERMISSIONS -> Icons.Rounded.Security
            HISTORY -> Icons.Rounded.Analytics
        }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 启动前台保活服务
        KeepAliveForegroundService.startService(this)

        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    var currentTab by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    // Android 13/14 通知权限动态请求
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAboutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = WarmSurface,
                        border = BorderStroke(1.dp, WarmBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = currentTab.icon,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = currentTab.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WarmTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "查看软件版本及关于信息",
                                tint = WarmTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmBackground,
                    titleContentColor = WarmTextPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 4.dp,
                color = WarmSurface,
                border = BorderStroke(0.5.dp, WarmBorderLight)
            ) {
                NavigationBar(
                    containerColor = WarmSurface,
                    contentColor = WarmTextPrimary
                ) {
                    MainDestination.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TerracottaPrimary,
                                selectedTextColor = TerracottaPrimary,
                                unselectedIconColor = WarmTextSecondary,
                                unselectedTextColor = WarmTextSecondary,
                                indicatorColor = TerracottaPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    MainDestination.HOME -> HomeScreen(
                        onNavigateToPermissions = { currentTab = MainDestination.PERMISSIONS },
                        onNavigateToBlacklist = { currentTab = MainDestination.BLACKLIST },
                        onNavigateToAISettings = { currentTab = MainDestination.AI_SETTINGS }
                    )
                    MainDestination.BLACKLIST -> BlacklistScreen()
                    MainDestination.AI_SETTINGS -> AISettingsScreen()
                    MainDestination.PERMISSIONS -> PermissionGuideScreen()
                    MainDestination.HISTORY -> StatisticsScreen()
                }
            }
        }
    }
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val (versionName, versionCode) = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName ?: "1.3"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
            Pair(vName, vCode)
        } catch (e: Exception) {
            Pair("1.3", "4")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标外框 (陶土渐变)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(TerracottaPrimary, CaramelSecondary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "AI 自律守护",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerracottaPrimaryContainer,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "v$versionName (Build $versionCode) • 正式版",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TerracottaPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 核心理念卡片
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmSurfaceContainer,
                    border = BorderStroke(0.8.dp, WarmBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = SageGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "核心理念",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "基于 AI 多模态大模型审查的硬核防沉迷工具。当受保护应用被打开时，严格由 AI 审查官评估开启理由与时长，重获专注与时间主控权。",
                            fontSize = 12.sp,
                            color = WarmTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 架构与特性标签
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmSurfaceContainer,
                    border = BorderStroke(0.8.dp, WarmBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureItem(title = "AI 审查机制", desc = "智能推理放行时长与客观评语")
                        FeatureItem(title = "自律复盘分析", desc = "多维度统计与 AI 自律导师评估")
                        FeatureItem(title = "双重保活守护", desc = "前台常驻服务 + 无障碍无感拦截")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "我知道了",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = WarmSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun FeatureItem(title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SageGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$title：",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WarmTextPrimary
        )
        Text(
            text = desc,
            fontSize = 12.sp,
            color = WarmTextSecondary
        )
    }
}