package com.example.myapplication.ui.screens

import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.ai.CloudOpenAIEngine
import com.example.myapplication.ai.LlamaCppEngine
import com.example.myapplication.data.model.AIEngineType
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSender
import com.example.myapplication.data.model.CloudProviderConfig
import com.example.myapplication.data.model.DecisionType
import com.example.myapplication.data.model.EvaluationResult
import com.example.myapplication.util.DeviceInfoHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewerEngineConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    val currentEngineType by repository.engineType.collectAsState(initial = AIEngineType.CLOUD)
    val activeConfig by repository.activeCloudConfig.collectAsState(initial = CloudProviderConfig.DEFAULT)
    val modelPath by repository.modelPath.collectAsState(initial = "/sdcard/Download/qwen2.5-3b-instruct-q4_k_m.gguf")
    val isTestModeEnabled by repository.isTestModeEnabled.collectAsState(initial = false)

    var selectedEngineType by remember(currentEngineType) { mutableStateOf(currentEngineType) }

    var apiKey by remember(activeConfig) { mutableStateOf(activeConfig.apiKey) }
    var baseUrl by remember(activeConfig) { mutableStateOf(activeConfig.baseUrl) }
    var modelName by remember(activeConfig) { mutableStateOf(activeConfig.modelName) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthResult by remember { mutableStateOf<com.example.myapplication.data.model.ApiHealthResult?>(null) }

    // 本地 GGUF 状态
    var modelPathInput by remember(modelPath) { mutableStateOf(modelPath) }
    var fileExists by remember { mutableStateOf(false) }
    var fileSizeStr by remember { mutableStateOf("0 MB") }
    var isGgufValid by remember { mutableStateOf(false) }
    var fileErrorMessage by remember { mutableStateOf<String?>(null) }
    var hasAllFilesAccess by remember { mutableStateOf(true) }

    val deviceBrandModel = remember { DeviceInfoHelper.getDeviceBrandAndModel() }
    val socProcessorName = remember { DeviceInfoHelper.getSocProcessorName() }
    val totalRamFormatted = remember { DeviceInfoHelper.getTotalMemoryFormatted(context) }

    fun checkStoragePermission() {
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun checkGgufFile(inputPath: String) {
        fileErrorMessage = null
        try {
            var cleanPath = inputPath.trim().removeSurrounding("\"").removeSurrounding("'")
            var file = File(cleanPath)

            if (!file.exists() && cleanPath.startsWith("/sdcard/")) {
                val rel = cleanPath.removePrefix("/sdcard/")
                val extFile = File(Environment.getExternalStorageDirectory(), rel)
                if (extFile.exists()) {
                    file = extFile
                    cleanPath = extFile.absolutePath
                }
            }

            fileExists = file.exists()
            if (fileExists && file.isFile) {
                val bytes = file.length()
                val mb = bytes / (1024.0 * 1024.0)
                fileSizeStr = String.format("%.1f MB", mb)

                val fis = FileInputStream(file)
                val header = ByteArray(4)
                val read = fis.read(header)
                fis.close()

                if (read == 4 && header[0] == 'G'.code.toByte() && header[1] == 'G'.code.toByte() &&
                    header[2] == 'U'.code.toByte() && header[3] == 'F'.code.toByte()
                ) {
                    isGgufValid = true
                    fileErrorMessage = null
                } else {
                    isGgufValid = false
                    fileErrorMessage = "非合法 GGUF 模型文件 (魔数不匹配)"
                }
            } else {
                fileSizeStr = "文件不存在"
                isGgufValid = false
                fileErrorMessage = if (!hasAllFilesAccess) "未开启所有文件访问权限，无法读取外部存储" else "路径指向的文件不存在"
            }
        } catch (e: Exception) {
            fileExists = false
            isGgufValid = false
            fileSizeStr = "读取异常"
            fileErrorMessage = e.message ?: "未知错误"
        }
    }

    LaunchedEffect(Unit) {
        checkStoragePermission()
        checkGgufFile(modelPathInput)
    }

    // SAF 选择文件
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = uri.path ?: ""
            val docId = if (path.contains(":")) path.substringAfterLast(":") else path
            val actualPath = if (docId.startsWith("/storage/")) docId else "/sdcard/$docId"
            modelPathInput = actualPath
            checkGgufFile(actualPath)
        }
    }

    // 推理测试台状态
    val localTestEngine = remember { LlamaCppEngine() }
    val cloudTestEngine = remember { CloudOpenAIEngine() }
    var isTestingInference by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<EvaluationResult?>(null) }
    var testTargetAppInput by remember { mutableStateOf("Bilibili") }
    var testReasonInput by remember { mutableStateOf("我要看昨天收藏的Android架构教程") }
    val testAppPresets = remember { listOf("Bilibili", "淘宝", "抖音", "微信", "小红书", "知乎") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141824),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 审查官引擎配置",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isTestModeEnabled) Color(0x33FFB300) else Color(0x3300E676),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTestModeEnabled) "🧪 测试环境" else "🛡️ 正式环境",
                        fontSize = 10.sp,
                        color = if (isTestModeEnabled) Color(0xFFFFB300) else Color(0xFF00E676),
                        fontWeight = FontWeight.Bold
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
                    text = "配置应用拦截弹窗时调用的 AI 审查官模型（支持任意兼容 OpenAI 的云端大模型与端侧离线引擎）：",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 引擎模式切换
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIEngineType.entries.forEach { engine ->
                        val isSelected = selectedEngineType == engine
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedEngineType = engine },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1B2335) else Color(0xFF0E111C)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedEngineType = engine },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (engine == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF)
                                    )
                                )
                                Text(
                                    text = if (engine == AIEngineType.CLOUD) "云端大模型" else "端侧离线",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF90A4AE)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedEngineType == AIEngineType.CLOUD) {
                    // =================== 云端大模型设置 (通用 OpenAI 兼容) ===================
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key", fontSize = 12.sp) },
                        placeholder = { Text("请输入 API Key (sk-...)", fontSize = 12.sp, color = Color(0xFF546E7A)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF78909C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF263238),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("API Base URL", fontSize = 12.sp) },
                        placeholder = { Text(CloudProviderConfig.DEFAULT_BASE_URL, fontSize = 12.sp, color = Color(0xFF546E7A)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF263238),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name", fontSize = 12.sp) },
                        placeholder = { Text(CloudProviderConfig.DEFAULT_MODEL_NAME, fontSize = 12.sp, color = Color(0xFF546E7A)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF263238),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (apiKey.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF37474F)
                            )
                        ) {
                            if (isCheckingHealth) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Color(0xFF00E5FF),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("正在探测...", fontSize = 11.sp, color = Color(0xFF00E5FF))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.FlashOn,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF78909C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡ 测通 API 连通性",
                                    fontSize = 11.sp,
                                    color = if (apiKey.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF78909C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (healthResult != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val res = healthResult!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (res.success) Color(0x2200E676) else Color(0x22FF5252),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (res.success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (res.success) Color(0xFF00E676) else Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = res.message,
                                fontSize = 11.sp,
                                color = if (res.success) Color(0xFF00E676) else Color(0xFFFF5252),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // =================== 端侧离线引擎设置 ===================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E111C))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "$deviceBrandModel ($socProcessorName)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "系统可用运存: $totalRamFormatted",
                                    fontSize = 11.sp,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelPathInput,
                        onValueChange = {
                            modelPathInput = it
                            checkGgufFile(it)
                        },
                        label = { Text("GGUF 离线模型路径", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = "选择文件",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0xFF263238),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 文件校验状态
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0E111C), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGgufValid) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (isGgufValid) Color(0xFF00E676) else Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGgufValid) "模型已就绪 (大小: $fileSizeStr)" else (fileErrorMessage ?: "模型未加载"),
                            fontSize = 11.sp,
                            color = if (isGgufValid) Color(0xFF00E676) else Color(0xFFFF5252)
                        )
                    }
                }

                // =================== 现场推理测试台 (受测试模式总开关控制) ===================
                if (isTestModeEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2335))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🧪 现场推理审查测试",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // 预设 App 标签
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                testAppPresets.forEach { app ->
                                    FilterChip(
                                        selected = testTargetAppInput == app,
                                        onClick = { testTargetAppInput = app },
                                        label = { Text(app, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00E5FF),
                                            selectedLabelColor = Color(0xFF0D0F18),
                                            containerColor = Color(0xFF0E111C),
                                            labelColor = Color(0xFF90A4AE)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = testReasonInput,
                                onValueChange = { testReasonInput = it },
                                label = { Text("测试申请使用理由", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF263238),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isTestingInference = true
                                        testResult = null

                                        val systemPrompt = repository.getEffectiveSystemPromptForApp(testTargetAppInput, testTargetAppInput)
                                        val activeEngine = if (selectedEngineType == AIEngineType.CLOUD) {
                                            cloudTestEngine.apply {
                                                this.apiKey = apiKey.trim()
                                                this.baseUrl = baseUrl.trim().ifBlank { CloudProviderConfig.DEFAULT_BASE_URL }
                                                this.modelName = modelName.trim().ifBlank { CloudProviderConfig.DEFAULT_MODEL_NAME }
                                            }
                                        } else {
                                            localTestEngine.apply {
                                                if (isGgufValid) preheat(modelPathInput)
                                            }
                                        }

                                        val result = activeEngine.evaluateConversation(
                                            conversationHistory = listOf(ChatMessage(sender = ChatSender.USER, text = testReasonInput)),
                                            targetAppName = testTargetAppInput,
                                            systemPrompt = systemPrompt
                                        )
                                        isTestingInference = false
                                        testResult = result
                                    }
                                },
                                enabled = !isTestingInference,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color(0xFF0D0F18)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingInference) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("正在审查判定中...", fontSize = 11.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("执行现场推理测试", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (testResult != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val res = testResult!!
                                val isAllow = res.decision == DecisionType.ALLOW
                                val isRetry = res.decision == DecisionType.RETRY
                                val statusColor = if (isAllow) Color(0xFF00E676) else if (isRetry) Color(0xFFFFB300) else Color(0xFFFF5252)
                                val statusText = if (isAllow) "【放行 ALLOW】" else if (isRetry) "【追问 RETRY】" else "【驳回 DENY】"

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0E111C), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "$statusText 耗时: ${res.latencyMs}ms",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "评语: ${res.comment}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFECEFF1)
                                    )
                                    if (res.guidanceTip.isNotBlank()) {
                                        Text(
                                            text = "引导提示: ${res.guidanceTip}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF00E5FF)
                                        )
                                    }
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
                    scope.launch {
                        repository.setEngineType(selectedEngineType)
                        if (selectedEngineType == AIEngineType.CLOUD) {
                            if (isTestModeEnabled) {
                                repository.saveTestCloudConfig(
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    modelName = modelName
                                )
                            } else {
                                repository.saveCloudConfig(
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    modelName = modelName
                                )
                            }
                        } else {
                            repository.setModelPath(modelPathInput)
                        }
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF0D0F18)
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
                Text("取消", color = Color(0xFF90A4AE))
            }
        }
    )
}
