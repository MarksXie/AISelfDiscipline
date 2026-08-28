package com.example.myapplication.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppApplication
import com.example.myapplication.data.model.AIEngineType
import com.example.myapplication.data.model.CloudProviderConfig
import com.example.myapplication.data.model.PersonaProfile
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AISettingsScreen() {
    val repository = AppApplication.instance.repository
    val scope = rememberCoroutineScope()

    val engineType by repository.engineType.collectAsState(initial = AIEngineType.CLOUD)
    val activeCloudConfig by repository.activeCloudConfig.collectAsState(initial = CloudProviderConfig.DEFAULT)
    val modelPath by repository.modelPath.collectAsState(initial = "/sdcard/Download/qwen2.5-3b-instruct-q4_k_m.gguf")
    val isTestModeEnabled by repository.isTestModeEnabled.collectAsState(initial = false)

    val allPersonas by repository.allPersonas.collectAsState(initial = PersonaProfile.BUILT_IN_PERSONAS)
    val activePersona by repository.activePersona.collectAsState(initial = PersonaProfile.BUILT_IN_NEUTRAL)

    var showEngineConfigDialog by remember { mutableStateOf(false) }
    var showEditPersonaDialog by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<PersonaProfile?>(null) }
    var personaToDelete by remember { mutableStateOf<PersonaProfile?>(null) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F18))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶部标题区
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 审查官引擎与人格配置",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "自由切换云端千亿大模型或端侧离线引擎，管理专属审查官人设",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }

        // 2. 当前生效引擎概览卡片（带设置按钮）
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (engineType == AIEngineType.CLOUD) Color(0x2200E5FF) else Color(0x22B388FF),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (engineType == AIEngineType.CLOUD) Icons.Rounded.Cloud else Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = if (engineType == AIEngineType.CLOUD) Color(0xFF00E5FF) else Color(0xFFB388FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (engineType == AIEngineType.CLOUD) "云端大模型" else "端侧离线引擎",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isTestModeEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x33FFB300), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "测试",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showEngineConfigDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0D0F18)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("配置模型", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. 保存反馈提示
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

        // 4. 审查官人格矩阵标题与添加按钮
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "审查官人格矩阵 (支持自定义)",
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
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新建人格", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. 人格卡片列表
        items(allPersonas, key = { it.id }) { persona ->
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

    // 模型统一配置弹窗 (云端 + 端侧 + 推理测试台)
    if (showEngineConfigDialog) {
        ReviewerEngineConfigDialog(
            onDismiss = { showEngineConfigDialog = false }
        )
    }

    // 添加/编辑自定义人格弹窗
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
                        text = "填写人格信息，系统将自动注入 App 意图判定审查上下文：",
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
                        label = { Text("【核心任务】专属语气与审查侧重", fontSize = 12.sp) },
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
