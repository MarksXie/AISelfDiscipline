package com.example.myapplication.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myapplication.data.model.CloudProviderPreset
import com.example.myapplication.data.model.DecisionType
import com.example.myapplication.data.model.EvaluationResult
import com.example.myapplication.data.model.PersonaProfile
import com.example.myapplication.data.model.ReasonType
import com.example.myapplication.util.DeviceInfoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AISettingsScreen() {
    val context = LocalContext.current
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    // 动态设备硬件信息
    val deviceBrandModel = remember { DeviceInfoHelper.getDeviceBrandAndModel() }
    val socProcessorName = remember { DeviceInfoHelper.getSocProcessorName() }
    val totalRamFormatted = remember { DeviceInfoHelper.getTotalMemoryFormatted(context) }

    // 人格列表与当前激活项
    val allPersonas by repository.allPersonas.collectAsState(initial = PersonaProfile.BUILT_IN_PERSONAS)
    val activePersona by repository.activePersona.collectAsState(initial = PersonaProfile.BUILT_IN_NEUTRAL)

    val modelPath by repository.modelPath.collectAsState(initial = "/sdcard/Download/qwen2.5-3b-instruct-q4_k_m.gguf")

    // 引擎模式与各服务商独立配置状态
    val engineType by repository.engineType.collectAsState(initial = AIEngineType.CLOUD)
    val cloudConfigsMap by repository.cloudConfigsMap.collectAsState(initial = emptyMap())
    val cloudProvider by repository.cloudProvider.collectAsState(initial = CloudProviderPreset.DEEPSEEK)

    var modelPathInput by remember(modelPath) { mutableStateOf(modelPath) }

    var selectedProvider by remember(cloudProvider) { mutableStateOf(cloudProvider) }
    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf(CloudProviderPreset.DEEPSEEK.defaultBaseUrl) }
    var modelNameInput by remember { mutableStateOf(CloudProviderPreset.DEEPSEEK.defaultModel) }
    var enableThinkingInput by remember { mutableStateOf(false) }
    var thinkingKeyInput by remember { mutableStateOf(CloudProviderPreset.DEEPSEEK.defaultThinkingKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    fun syncInputsWithProvider(preset: CloudProviderPreset) {
        val cfg = cloudConfigsMap[preset.id]
        apiKeyInput = cfg?.apiKey ?: ""
        baseUrlInput = if (!cfg?.baseUrl.isNullOrBlank()) cfg!!.baseUrl else preset.defaultBaseUrl
        modelNameInput = if (!cfg?.modelName.isNullOrBlank()) cfg!!.modelName else preset.defaultModel
        enableThinkingInput = cfg?.enableThinking ?: false
        thinkingKeyInput = if (!cfg?.thinkingParamKey.isNullOrBlank()) cfg!!.thinkingParamKey else preset.defaultThinkingKey
    }

    LaunchedEffect(selectedProvider, cloudConfigsMap) {
        syncInputsWithProvider(selectedProvider)
    }

    var saveFeedback by remember { mutableStateOf<String?>(null) }

    // 自定义人格编辑弹窗状态
    var showEditPersonaDialog by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<PersonaProfile?>(null) }
    var personaToDelete by remember { mutableStateOf<PersonaProfile?>(null) }

    // 存储权限状态
    var hasAllFilesAccess by remember { mutableStateOf(true) }

    fun checkStoragePermission() {
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    // GGUF 文件状态检测
    var fileExists by remember { mutableStateOf(false) }
    var fileSizeStr by remember { mutableStateOf("0 MB") }
    var isGgufValid by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun checkGgufFile(inputPath: String) {
        errorMessage = null
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
                    errorMessage = null
                } else {
                    isGgufValid = false
                    errorMessage = "非合法 GGUF 模型文件 (魔数不匹配)"
                }
            } else {
                fileSizeStr = "文件不存在"
                isGgufValid = false
                errorMessage = if (!hasAllFilesAccess) "未开启所有文件访问权限，无法读取外部存储" else "路径指向的文件不存在"
            }
        } catch (e: Exception) {
            fileExists = false
            isGgufValid = false
            fileSizeStr = "读取异常"
            errorMessage = e.message ?: "未知错误"
        }
    }

    LaunchedEffect(Unit) {
        checkStoragePermission()
        checkGgufFile(modelPathInput)
    }

    // SAF 文件选择器
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

    // 推理测试台引擎
    val localTestEngine = remember { LlamaCppEngine() }
    val cloudTestEngine = remember { CloudOpenAIEngine() }
    var isTestingInference by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<EvaluationResult?>(null) }
    var testTargetAppInput by remember { mutableStateOf("Bilibili") }
    var testReasonInput by remember { mutableStateOf("我要看昨天收藏的Android教程") }
    val testAppPresets = remember { listOf("Bilibili", "淘宝", "抖音", "微信", "小红书", "知乎") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F18))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题区
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 审查官引擎与人格配置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "自由切换云端千亿大模型或端侧离线引擎，管理专属多审查官人设",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }

        // 真实硬件芯片与设备信息卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0x2200E5FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceBrandModel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "处理器: $socProcessorName  |  运存: $totalRamFormatted",
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }
            }
        }

        // 核心：引擎模式切换分段卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "推理引擎模式 (可随时手动切换)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ☁️ 云端大模型选项
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (engineType == AIEngineType.CLOUD) Color(0x3300E5FF) else Color(0xFF1E2336))
                                .border(
                                    1.dp,
                                    if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        repository.setEngineType(AIEngineType.CLOUD)
                                        saveFeedback = "已切换为：云端大模型模式"
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cloud,
                                        contentDescription = null,
                                        tint = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "云端大模型",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "千亿级极智推理，秒懂生活刚需，零本地发热",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // ⚡ 端侧离线引擎选项
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (engineType == AIEngineType.LOCAL_GGUF) Color(0x33B388FF) else Color(0xFF1E2336))
                                .border(
                                    1.dp,
                                    if (engineType == AIEngineType.LOCAL_GGUF) Color(0xFFB388FF) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        repository.setEngineType(AIEngineType.LOCAL_GGUF)
                                        saveFeedback = "已切换为：端侧离线引擎模式"
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Memory,
                                        contentDescription = null,
                                        tint = if (engineType == AIEngineType.LOCAL_GGUF) Color(0xFFB388FF) else Color(0xFF90A4AE),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "端侧离线引擎",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (engineType == AIEngineType.LOCAL_GGUF) Color(0xFFB388FF) else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "llama.cpp 本地运行 Qwen-3B，完全离线私密",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 分支 1：云端大模型配置卡片
        if (engineType == AIEngineType.CLOUD) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "云端 API 服务商预设与授权",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 服务商预设 Chips
                        Text(
                            text = "选择主流服务商快速填充：",
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CloudProviderPreset.entries.forEach { preset ->
                                val isSelected = selectedProvider == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2336))
                                        .clickable {
                                            selectedProvider = preset
                                            syncInputsWithProvider(preset)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = preset.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0D0F18) else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // API Key
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("${selectedProvider.title} API Key", fontSize = 12.sp) },
                            placeholder = { Text("例如：sk-xxxxxx", fontSize = 12.sp) },
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFF90A4AE),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            apiKeyInput = clip.getItemAt(0).text.toString().trim()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = "粘贴",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF23283B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Base URL
                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it },
                            label = { Text("API Base URL (OpenAI 兼容路径)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF23283B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Name
                        OutlinedTextField(
                            value = modelNameInput,
                            onValueChange = { modelNameInput = it },
                            label = { Text("Model Name (大模型名称)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF23283B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 深度思考 / 推理模式控制卡片
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2336), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🧠 开启深度思考 / 推理模式",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "向 API 发送思考参数，深度推演使用意图",
                                            fontSize = 11.sp,
                                            color = Color(0xFF90A4AE)
                                        )
                                    }
                                    androidx.compose.material3.Switch(
                                        checked = enableThinkingInput,
                                        onCheckedChange = { enableThinkingInput = it },
                                        colors = androidx.compose.material3.SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF0D0F18),
                                            checkedTrackColor = Color(0xFF00E5FF)
                                        )
                                    )
                                }

                                androidx.compose.animation.AnimatedVisibility(visible = enableThinkingInput) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        OutlinedTextField(
                                            value = thinkingKeyInput,
                                            onValueChange = { thinkingKeyInput = it },
                                            label = { Text("思考参数键名 (Thinking Param Key)", fontSize = 12.sp) },
                                            placeholder = { Text("如：${selectedProvider.defaultThinkingKey}", fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF00E5FF),
                                                unfocusedBorderColor = Color(0xFF37474F),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "💡 通义千问为 enable_thinking，DeepSeek / 硅基流动为 thinking",
                                            fontSize = 10.sp,
                                            color = Color(0xFF80D8FF)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    repository.saveCloudConfig(
                                        provider = selectedProvider,
                                        apiKey = apiKeyInput,
                                        baseUrl = baseUrlInput,
                                        modelName = modelNameInput,
                                        enableThinking = enableThinkingInput,
                                        thinkingParamKey = thinkingKeyInput
                                    )
                                    saveFeedback = "已保存并激活【${selectedProvider.title}】专属配置！"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0D0F18)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存并激活【${selectedProvider.title}】", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // 分支 2：端侧本地 GGUF 配置卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "端侧 GGUF 模型文件路径 (llama.cpp JNI)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = modelPathInput,
                            onValueChange = {
                                modelPathInput = it
                                checkGgufFile(it)
                            },
                            label = { Text("本地 GGUF 绝对路径", fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = "选择文件",
                                        tint = Color(0xFFB388FF)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFB388FF),
                                unfocusedBorderColor = Color(0xFF23283B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isGgufValid) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = if (isGgufValid) Color(0xFF00E676) else Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGgufValid) "GGUF 文件合法 ($fileSizeStr)" else "状态：$fileSizeStr",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGgufValid) Color(0xFF00E676) else Color(0xFFFFB300)
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.setModelPath(modelPathInput)
                                        withContext(Dispatchers.IO) {
                                            checkGgufFile(modelPathInput)
                                        }
                                        saveFeedback = "端侧模型路径已保存"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB388FF),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("保存并校验", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (errorMessage != null && !isGgufValid) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage!!,
                                fontSize = 11.sp,
                                color = Color(0xFFFF8A80),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 推理测速与实时问答体验台
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (engineType == AIEngineType.CLOUD) "云端大模型在线实时测速与审阅" else "端侧模型离线基准测速",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (testResult != null) {
                            Text(
                                text = "耗时: ${testResult?.latencyMs} ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF)
                            )
                        }
                    }

                    // 目标 App 快捷预设 Chips
                    Text(
                        text = "选择测试目标 App 预设：",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        testAppPresets.forEach { appName ->
                            val isSelected = testTargetAppInput.equals(appName, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) (if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF)) else Color(0xFF1E2336))
                                    .clickable {
                                        testTargetAppInput = appName
                                        if (appName == "Bilibili") testReasonInput = "我要看昨天收藏的Android教程"
                                        else if (appName == "淘宝") testReasonInput = "想买点抽纸"
                                        else if (appName == "微信") testReasonInput = "回复同事的工作消息"
                                        else if (appName == "抖音") testReasonInput = "无聊随便刷刷"
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = appName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0D0F18) else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testTargetAppInput,
                        onValueChange = { testTargetAppInput = it },
                        label = { Text("测试目标 App 名称", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0xFF23283B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testReasonInput,
                        onValueChange = {
                            if (it.length <= 300) {
                                testReasonInput = it
                            }
                        },
                        label = { Text("测试输入理由 (最多300字)", fontSize = 12.sp) },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${testReasonInput.length}/300",
                                    fontSize = 10.sp,
                                    color = if (testReasonInput.length >= 280) Color(0xFFFFB300) else Color(0xFF78909C)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0xFF23283B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isTestingInference = true
                            scope.launch {
                                val targetAppName = testTargetAppInput.trim().ifBlank { "Bilibili" }
                                val systemPrompt = repository.getEffectiveSystemPromptForApp("", targetAppName)
                                val result = if (engineType == AIEngineType.CLOUD) {
                                    cloudTestEngine.apiKey = apiKeyInput
                                    cloudTestEngine.baseUrl = baseUrlInput
                                    cloudTestEngine.modelName = modelNameInput
                                    cloudTestEngine.enableThinking = enableThinkingInput
                                    cloudTestEngine.thinkingParamKey = thinkingKeyInput
                                    cloudTestEngine.evaluateReason(
                                        reason = testReasonInput,
                                        targetAppName = targetAppName,
                                        systemPrompt = systemPrompt
                                    )
                                } else {
                                    localTestEngine.evaluateReason(
                                        reason = testReasonInput,
                                        targetAppName = targetAppName,
                                        systemPrompt = systemPrompt
                                    )
                                }
                                testResult = result
                                isTestingInference = false
                            }
                        },
                        enabled = !isTestingInference,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingInference) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI 推理中...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (engineType == AIEngineType.CLOUD) "发起云端推理与多轮问答测速" else "运行端侧推理测速 (GBNF 约束)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (testResult != null) {
                        val result = testResult!!
                        val statusColor = when (result.decision) {
                            DecisionType.ALLOW -> Color(0xFF00E676)
                            DecisionType.RETRY -> Color(0xFF00E5FF)
                            DecisionType.DENY -> Color(0xFFFF1744)
                        }
                        val decisionTitle = when (result.decision) {
                            DecisionType.ALLOW -> "决策：ALLOW (放行)"
                            DecisionType.RETRY -> "决策：RETRY (追问引导)"
                            DecisionType.DENY -> "决策：DENY (拦截驳回)"
                        }
                        val reasonTypeLabel = when (result.reasonType) {
                            ReasonType.SPECIFIC_PURPOSE -> "具体明确目的"
                            ReasonType.VAGUE_PURPOSE -> "目的模糊"
                            ReasonType.IMPULSIVE_USE -> "冲动无聊消遣"
                            ReasonType.HABITUAL_USE -> "习惯性打开"
                            ReasonType.APP_MISMATCH -> "App功能不匹配"
                            ReasonType.OTHER -> "其他"
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    statusColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (result.decision) {
                                                DecisionType.ALLOW -> Icons.Rounded.CheckCircle
                                                DecisionType.RETRY -> Icons.Rounded.QuestionAnswer
                                                DecisionType.DENY -> Icons.Rounded.Block
                                            },
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = decisionTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1E2336), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = reasonTypeLabel,
                                            fontSize = 10.sp,
                                            color = Color(0xFF90CAF9)
                                        )
                                    }
                                }

                                if (result.guidanceTip.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "💡 AI动态指引：${result.guidanceTip}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF80D8FF),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (result.decision == DecisionType.RETRY) "追问内容：${result.comment}" else "AI 评语：${result.comment}",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 保存全局反馈
        if (saveFeedback != null) {
            item {
                Text(
                    text = saveFeedback!!,
                    color = Color(0xFF00E676),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // 审查官人格选择与自定义管理
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "审查官人格矩阵 (支持自定义多审查官)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        editingPersona = null
                        showEditPersonaDialog = true
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新建人格", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(allPersonas) { persona ->
            val isSelected = activePersona.id == persona.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            repository.setActivePersonaId(persona.id)
                            saveFeedback = "已激活审查官：${persona.title}"
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1A2238) else Color(0xFF141824)
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            scope.launch {
                                repository.setActivePersonaId(persona.id)
                                saveFeedback = "已激活审查官：${persona.title}"
                            }
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF00E5FF),
                            unselectedColor = Color(0xFF546E7A)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = persona.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White
                            )
                            if (persona.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF263238), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("自定义", fontSize = 9.sp, color = Color(0xFF80D8FF))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = persona.desc,
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE),
                            lineHeight = 16.sp
                        )
                    }

                    if (persona.isCustom) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                editingPersona = persona
                                showEditPersonaDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "编辑",
                                    tint = Color(0xFF90A4AE),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = {
                                personaToDelete = persona
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "删除",
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑自定义人格弹窗 (受控输入 title, desc, coreTask)
    if (showEditPersonaDialog) {
        var inputTitle by remember { mutableStateOf(editingPersona?.title ?: "") }
        var inputDesc by remember { mutableStateOf(editingPersona?.desc ?: "") }
        var inputCoreTask by remember { mutableStateOf(editingPersona?.coreTask ?: "") }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditPersonaDialog = false },
            title = {
                Text(
                    text = if (editingPersona == null) "新建自定义审查官" else "编辑自定义审查官",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "只需填写以下 3 项，系统将自动拼装入标准「App 意图判断器」安全框架：",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 14.sp
                    )

                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("人格名称 (Title)", fontSize = 12.sp) },
                        placeholder = { Text("例如：苏格拉底导师 / 健身教练", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF23283B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        label = { Text("简短描述 (Desc)", fontSize = 12.sp) },
                        placeholder = { Text("例如：哲学反思发问，探究动机本质", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF23283B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = inputCoreTask,
                        onValueChange = { inputCoreTask = it },
                        label = { Text("【核心任务】/ 专属语气与审查侧重", fontSize = 12.sp) },
                        placeholder = { Text("例如：以哲学家的反问口吻启发用户审视打开App的真实目的，对摸鱼予以反思引导，对明确事项痛快放行。", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF23283B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTitle.isBlank()) {
                            dialogError = "请填写人格名称"
                            return@Button
                        }
                        if (inputCoreTask.isBlank()) {
                            dialogError = "请填写【核心任务】"
                            return@Button
                        }

                        val targetId = editingPersona?.id ?: UUID.randomUUID().toString()
                        val newProfile = PersonaProfile(
                            id = targetId,
                            title = inputTitle.trim(),
                            desc = inputDesc.trim().ifBlank { "自定义审查官人设" },
                            coreTask = inputCoreTask.trim(),
                            isCustom = true
                        )

                        scope.launch {
                            repository.saveCustomPersona(newProfile)
                            saveFeedback = "已保存并激活自定义审查官：${newProfile.title}"
                        }
                        showEditPersonaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0D0F18)
                    )
                ) {
                    Text("保存生效", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditPersonaDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF1E2336),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 删除确认弹窗
    if (personaToDelete != null) {
        val target = personaToDelete!!
        AlertDialog(
            onDismissRequest = { personaToDelete = null },
            title = { Text("确认删除审查官？", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "确定要删除自定义人格【${target.title}】吗？删除后不可恢复。",
                    color = Color(0xFF90A4AE),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteCustomPersona(target.id)
                            saveFeedback = "已删除自定义人格：${target.title}"
                        }
                        personaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                ) {
                    Text("确认删除", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { personaToDelete = null }) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF1E2336),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
