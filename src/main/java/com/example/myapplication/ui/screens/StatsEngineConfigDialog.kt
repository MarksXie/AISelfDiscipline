package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.ai.StatsAIAnalyzer
import com.example.myapplication.data.model.AIEngineType
import com.example.myapplication.data.model.CloudProviderConfig
import com.example.myapplication.data.model.StatsPeriodType
import com.example.myapplication.ui.theme.AmberWarm
import com.example.myapplication.ui.theme.AmberWarmContainer
import com.example.myapplication.ui.theme.CaramelSecondary
import com.example.myapplication.ui.theme.CoralDanger
import com.example.myapplication.ui.theme.CoralDangerContainer
import com.example.myapplication.ui.theme.OnAmberWarmContainer
import com.example.myapplication.ui.theme.OnCoralDangerContainer
import com.example.myapplication.ui.theme.OnSageGreenContainer
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsEngineConfigDialog(
    onDismiss: () -> Unit,
    onReportGenerated: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    val currentEngineType by repository.statsEngineType.collectAsState(initial = AIEngineType.CLOUD)
    val activeConfig by repository.statsActiveCloudConfig.collectAsState(initial = CloudProviderConfig.DEFAULT)
    val isTestModeEnabled by repository.isTestModeEnabled.collectAsState(initial = false)

    var selectedEngineType by remember(currentEngineType) { mutableStateOf(currentEngineType) }

    var apiKey by remember(activeConfig) { mutableStateOf(activeConfig.apiKey) }
    var baseUrl by remember(activeConfig) { mutableStateOf(activeConfig.baseUrl) }
    var modelName by remember(activeConfig) { mutableStateOf(activeConfig.modelName) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val cloudTestEngine = remember { com.example.myapplication.ai.CloudOpenAIEngine() }
    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthResult by remember { mutableStateOf<com.example.myapplication.data.model.ApiHealthResult?>(null) }

    var isTesting by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WarmSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "自律统计 AI 引擎配置",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isTestModeEnabled) AmberWarmContainer else SageGreenContainer
                ) {
                    Text(
                        text = if (isTestModeEnabled) "🧪 测试环境" else "🛡️ 正式环境",
                        fontSize = 10.sp,
                        color = if (isTestModeEnabled) OnAmberWarmContainer else OnSageGreenContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "用于定期生成日/周/月/季/半年/年报的独立 AI 大模型配置（与拦截审查官完全隔离独立，支持任意 OpenAI 兼容模型）：",
                    fontSize = 12.sp,
                    color = WarmTextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 引擎模式选择
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIEngineType.entries.forEach { engine ->
                        val isSelected = selectedEngineType == engine
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedEngineType = engine },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TerracottaPrimaryContainer else WarmSurfaceContainer,
                            border = BorderStroke(1.dp, if (isSelected) TerracottaPrimary else WarmBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedEngineType = engine },
                                    colors = RadioButtonDefaults.colors(selectedColor = TerracottaPrimary)
                                )
                                Text(
                                    text = if (engine == AIEngineType.CLOUD) "云端大模型" else "端侧离线",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TerracottaPrimary else WarmTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedEngineType == AIEngineType.CLOUD) {
                    // API Key 输入框
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key", fontSize = 12.sp) },
                        placeholder = { Text("请输入 API Key (sk-...)", fontSize = 12.sp, color = WarmTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = WarmTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = WarmTextPrimary,
                            unfocusedTextColor = WarmTextPrimary,
                            focusedContainerColor = WarmSurface,
                            unfocusedContainerColor = WarmSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Base URL 输入框
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("API Base URL", fontSize = 12.sp) },
                        placeholder = { Text(CloudProviderConfig.DEFAULT_BASE_URL, fontSize = 12.sp, color = WarmTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = WarmTextPrimary,
                            unfocusedTextColor = WarmTextPrimary,
                            focusedContainerColor = WarmSurface,
                            unfocusedContainerColor = WarmSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Model Name 输入框
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name", fontSize = 12.sp) },
                        placeholder = { Text(CloudProviderConfig.DEFAULT_MODEL_NAME, fontSize = 12.sp, color = WarmTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = WarmTextPrimary,
                            unfocusedTextColor = WarmTextPrimary,
                            focusedContainerColor = WarmSurface,
                            unfocusedContainerColor = WarmSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 测通 API 连通性操作栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isCheckingHealth = true
                                    healthResult = null
                                    val result = cloudTestEngine.testConnection(
                                        testApiKey = apiKey,
                                        testBaseUrl = baseUrl,
                                        testModelName = modelName
                                    )
                                    isCheckingHealth = false
                                    healthResult = result
                                }
                            },
                            enabled = !isCheckingHealth && apiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (apiKey.isNotBlank()) TerracottaPrimary else WarmBorder
                            )
                        ) {
                            if (isCheckingHealth) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = TerracottaPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("正在探测...", fontSize = 11.sp, color = TerracottaPrimary)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.FlashOn,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) TerracottaPrimary else WarmTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡ 测通 API 连通性",
                                    fontSize = 11.sp,
                                    color = if (apiKey.isNotBlank()) TerracottaPrimary else WarmTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (healthResult != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val res = healthResult!!
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (res.success) SageGreenContainer else CoralDangerContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (res.success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (res.success) SageGreen else CoralDanger,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = res.message,
                                    fontSize = 11.sp,
                                    color = if (res.success) OnSageGreenContainer else OnCoralDangerContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = WarmSurfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = SageGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "端侧离线引擎",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "使用本地 llama.cpp 离线推理模型。提示：端侧 3B 模型长文本总结能力较轻量，推荐云端大模型以获得更深刻的自律导师洞见。",
                                fontSize = 11.sp,
                                color = WarmTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // 开发者测试模式下的测试生成报告按钮与结果展示
                if (isTestModeEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = WarmSurfaceContainer,
                        border = BorderStroke(1.dp, WarmBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🧪 开发者测试生成",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isTesting = true
                                            testResultText = null
                                            isTestError = false

                                            // 临时保存当前配置以便测试
                                            repository.setStatsEngineType(selectedEngineType)
                                            if (selectedEngineType == AIEngineType.CLOUD) {
                                                if (isTestModeEnabled) {
                                                    repository.saveTestStatsCloudConfig(
                                                        apiKey = apiKey,
                                                        baseUrl = baseUrl,
                                                        modelName = modelName
                                                    )
                                                } else {
                                                    repository.saveStatsCloudConfig(
                                                        apiKey = apiKey,
                                                        baseUrl = baseUrl,
                                                        modelName = modelName
                                                    )
                                                }
                                            }

                                            val testReport = StatsAIAnalyzer.generateReport(
                                                context = context,
                                                periodType = StatsPeriodType.DAY,
                                                offset = 0
                                            )
                                            isTesting = false
                                            isTestError = testReport.isError
                                            testResultText = if (testReport.isError) {
                                                "测试失败：${testReport.errorMessage}"
                                            } else {
                                                "测试生成成功！\n" + testReport.aiEvaluation.take(200) + "..."
                                            }
                                            if (!testReport.isError) {
                                                onReportGenerated()
                                            }
                                        }
                                    },
                                    enabled = !isTesting,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TerracottaPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("正在生成...", fontSize = 11.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("测试生成当日报告", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (testResultText != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = testResultText ?: "",
                                    fontSize = 11.sp,
                                    color = if (isTestError) CoralDanger else SageGreen,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        repository.setStatsEngineType(selectedEngineType)
                        if (selectedEngineType == AIEngineType.CLOUD) {
                            if (isTestModeEnabled) {
                                repository.saveTestStatsCloudConfig(
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    modelName = modelName
                                )
                            } else {
                                repository.saveStatsCloudConfig(
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    modelName = modelName
                                )
                            }
                        }
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存设置", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("取消", color = WarmTextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
