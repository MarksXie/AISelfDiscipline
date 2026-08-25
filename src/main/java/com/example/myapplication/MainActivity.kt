package com.example.myapplication

import android.Manifest
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myapplication.service.KeepAliveForegroundService
import com.example.myapplication.ui.screens.AISettingsScreen
import com.example.myapplication.ui.screens.BlacklistScreen
import com.example.myapplication.ui.screens.HistoryScreen
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.PermissionGuideScreen
import com.example.myapplication.ui.theme.DarkBg
import com.example.myapplication.ui.theme.MyApplicationTheme

enum class MainDestination(val label: String) {
    HOME("首页守护"),
    BLACKLIST("受保护应用"),
    AI_SETTINGS("AI审查官"),
    PERMISSIONS("权限保活"),
    HISTORY("自律周报");

    val icon: ImageVector
        get() = when (this) {
            HOME -> Icons.Rounded.Shield
            BLACKLIST -> Icons.Rounded.AppBlocking
            AI_SETTINGS -> Icons.Rounded.Psychology
            PERMISSIONS -> Icons.Rounded.Security
            HISTORY -> Icons.Rounded.History
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

    // Android 13/14 通知权限动态请求
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10131E),
                contentColor = Color.White
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
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF78909C),
                            unselectedTextColor = Color(0xFF78909C),
                            indicatorColor = Color(0xFF1B2335)
                        )
                    )
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
                    MainDestination.HISTORY -> HistoryScreen()
                }
            }
        }
    }
}