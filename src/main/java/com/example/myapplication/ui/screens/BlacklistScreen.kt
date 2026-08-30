package com.example.myapplication.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.myapplication.AppApplication
import com.example.myapplication.data.model.AppInfo
import com.example.myapplication.data.model.AppRuleProfile
import com.example.myapplication.ui.theme.CoralDanger
import com.example.myapplication.ui.theme.CoralDangerContainer
import com.example.myapplication.ui.theme.OnCoralDangerContainer
import com.example.myapplication.ui.theme.OnSageGreenContainer
import com.example.myapplication.ui.theme.SageGreen
import com.example.myapplication.ui.theme.SageGreenContainer
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlacklistScreen() {
    val context = LocalContext.current
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val blacklistedSet by repository.blacklistedPackages.collectAsState(initial = emptySet())

    var selectedAppForRules by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        allApps = repository.getInstalledApps()
        isLoading = false
    }

    val filteredApps = remember(allApps, searchQuery, blacklistedSet) {
        allApps.map { it.copy(isBlocked = blacklistedSet.contains(it.packageName)) }
            .filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(compareByDescending<AppInfo> { it.isBlocked }.thenBy { it.appName })
    }

    val isAllSelected = remember(filteredApps) {
        filteredApps.isNotEmpty() && filteredApps.all { it.isBlocked }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 头部标题与描述
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(TerracottaPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "防沉迷受保护应用",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmTextPrimary
                )
                Text(
                    text = "被拦截的应用打开时触发 AI 意图判定与专属自律规则",
                    fontSize = 11.sp,
                    color = WarmTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 搜索框（纯白暖底 + 1dp 细暖边框）
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索应用名称或包名...", fontSize = 13.sp, color = WarmTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = WarmTextSecondary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerracottaPrimary,
                unfocusedBorderColor = WarmBorder,
                focusedTextColor = WarmTextPrimary,
                unfocusedTextColor = WarmTextPrimary,
                focusedContainerColor = WarmSurface,
                unfocusedContainerColor = WarmSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 统计与快捷全选/全不选操作栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "已拦截 ${blacklistedSet.size} 个应用",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaPrimary
                )
                Text(
                    text = "共发现 ${allApps.size} 个应用",
                    fontSize = 11.sp,
                    color = WarmTextMuted
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        val targetPackages = filteredApps.map { it.packageName }
                        repository.setAllBlacklist(targetPackages, !isAllSelected)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllSelected) CoralDangerContainer else TerracottaPrimaryContainer,
                    contentColor = if (isAllSelected) CoralDanger else TerracottaPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = if (isAllSelected) Icons.Rounded.RemoveDone else Icons.Rounded.DoneAll,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAllSelected) "全都不选" else "全选",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TerracottaPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppItemCard(
                        app = app,
                        pm = context.packageManager,
                        onToggle = { shouldBlock ->
                            scope.launch {
                                repository.toggleBlacklist(app.packageName, shouldBlock)
                            }
                        },
                        onConfigureRules = {
                            selectedAppForRules = app
                        }
                    )
                }
            }
        }
    }

    // App 专属自律规则配置弹窗
    if (selectedAppForRules != null) {
        val targetApp = selectedAppForRules!!
        AppRulesDialog(
            app = targetApp,
            onDismiss = { selectedAppForRules = null }
        )
    }
}

@Composable
private fun AppItemCard(
    app: AppInfo,
    pm: PackageManager,
    onToggle: (Boolean) -> Unit,
    onConfigureRules: () -> Unit
) {
    val appIcon: Drawable? = remember(app.packageName) {
        try {
            pm.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x108D5B3E)
            ),
        shape = RoundedCornerShape(14.dp),
        color = WarmSurface,
        border = BorderStroke(
            1.dp,
            if (app.isBlocked) TerracottaPrimary.copy(alpha = 0.3f) else WarmBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    val bitmap = remember(appIcon) {
                        appIcon.toBitmap(width = 72, height = 72).asImageBitmap()
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(WarmSurfaceContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Android,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmTextPrimary
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        color = WarmTextMuted,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 规则配置按钮
                IconButton(
                    onClick = onConfigureRules,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "自律规则",
                        tint = if (app.isBlocked) TerracottaPrimary else WarmTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = { onToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TerracottaPrimary,
                        uncheckedThumbColor = WarmTextSecondary,
                        uncheckedTrackColor = WarmBorder
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppRulesDialog(
    app: AppInfo,
    onDismiss: () -> Unit
) {
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    var ruleProfile by remember { mutableStateOf<AppRuleProfile?>(null) }
    val allowedList = remember { mutableStateListOf<String>() }
    val forbiddenList = remember { mutableStateListOf<String>() }

    var newAllowedInput by remember { mutableStateOf("") }
    var newForbiddenInput by remember { mutableStateOf("") }

    LaunchedEffect(app.packageName) {
        val profile = repository.getEffectiveRuleForApp(app.packageName, app.appName)
        ruleProfile = profile
        allowedList.clear()
        allowedList.addAll(profile.allowedRules)
        forbiddenList.clear()
        forbiddenList.addAll(profile.forbiddenRules)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "【${app.appName}】专属自律规则",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WarmTextPrimary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "长期行为约束将随大模型 System Prompt 动态注入，引导精准判断：",
                        fontSize = 11.sp,
                        color = WarmTextSecondary,
                        lineHeight = 15.sp
                    )
                }

                // 1. 允许使用的正向目的 (应当 ALLOW)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = WarmSurfaceContainer,
                        border = BorderStroke(0.8.dp, SageGreen.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = SageGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "明确允许使用的正向目的 (ALLOW)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SageGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allowedList.forEach { rule ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SageGreenContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = rule, fontSize = 11.sp, color = OnSageGreenContainer)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "删除",
                                                tint = SageGreen,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { allowedList.remove(rule) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newAllowedInput,
                                    onValueChange = { newAllowedInput = it },
                                    placeholder = { Text("添加允许项 (如'观看指定收藏教程')", fontSize = 11.sp, color = WarmTextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SageGreen,
                                        unfocusedBorderColor = WarmBorder,
                                        focusedTextColor = WarmTextPrimary,
                                        unfocusedTextColor = WarmTextPrimary,
                                        focusedContainerColor = WarmSurface,
                                        unfocusedContainerColor = WarmSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        if (newAllowedInput.isNotBlank()) {
                                            allowedList.add(newAllowedInput.trim())
                                            newAllowedInput = ""
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "添加", tint = SageGreen)
                                }
                            }
                        }
                    }
                }

                // 2. 严禁放行的低质量消遣 (应当 DENY)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = WarmSurfaceContainer,
                        border = BorderStroke(0.8.dp, CoralDanger.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = CoralDanger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "严禁放行的低质量消遣 (DENY)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralDanger
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                forbiddenList.forEach { rule ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CoralDangerContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = rule, fontSize = 11.sp, color = OnCoralDangerContainer)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "删除",
                                                tint = CoralDanger,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { forbiddenList.remove(rule) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newForbiddenInput,
                                    onValueChange = { newForbiddenInput = it },
                                    placeholder = { Text("添加禁止项 (如'无目的刷推荐短视频')", fontSize = 11.sp, color = WarmTextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CoralDanger,
                                        unfocusedBorderColor = WarmBorder,
                                        focusedTextColor = WarmTextPrimary,
                                        unfocusedTextColor = WarmTextPrimary,
                                        focusedContainerColor = WarmSurface,
                                        unfocusedContainerColor = WarmSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        if (newForbiddenInput.isNotBlank()) {
                                            forbiddenList.add(newForbiddenInput.trim())
                                            newForbiddenInput = ""
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "添加", tint = CoralDanger)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = AppRuleProfile(
                        packageName = app.packageName,
                        appName = app.appName,
                        allowedRules = allowedList.toList(),
                        forbiddenRules = forbiddenList.toList(),
                        isCustom = true
                    )
                    scope.launch {
                        repository.saveAppRule(updated)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存专属规则", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.resetAppRule(app.packageName)
                            val def = repository.getEffectiveRuleForApp(app.packageName, app.appName)
                            allowedList.clear()
                            allowedList.addAll(def.allowedRules)
                            forbiddenList.clear()
                            forbiddenList.addAll(def.forbiddenRules)
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null, tint = WarmTextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置默认", color = WarmTextMuted)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                    Text("关闭", color = WarmTextSecondary)
                }
            }
        },
        containerColor = WarmSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
