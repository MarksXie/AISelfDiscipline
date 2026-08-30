package com.example.myapplication.ui.overlay

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSender
import com.example.myapplication.data.model.DecisionType
import com.example.myapplication.data.model.EvaluationResult
import com.example.myapplication.data.model.PersonaType
import com.example.myapplication.ui.theme.AmberWarm
import com.example.myapplication.ui.theme.AmberWarmContainer
import com.example.myapplication.ui.theme.CaramelSecondary
import com.example.myapplication.ui.theme.CaramelSecondaryContainer
import com.example.myapplication.ui.theme.CoralDanger
import com.example.myapplication.ui.theme.CoralDangerContainer
import com.example.myapplication.ui.theme.OnAmberWarmContainer
import com.example.myapplication.ui.theme.OnCoralDangerContainer
import com.example.myapplication.ui.theme.OnSageGreenContainer
import com.example.myapplication.ui.theme.OnTerracottaContainer
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

enum class OverlayStep {
    CONVERSATION_INQUIRY, // 理由陈述与多轮问询气泡对话流
    APPROVED_SELECT_TIME, // 终审通过：转盘时间选择 + 总结评语
    REJECTED_FEEDBACK     // 终审驳回：驳回卡片 + 批评总结
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterceptOverlayContent(
    targetPackageName: String,
    targetAppName: String,
    targetAppIcon: Drawable?,
    persona: PersonaType,
    personaTitle: String = persona.title,
    isModelReady: Boolean,
    isTest: Boolean = false,
    onEvaluateConversation: (history: List<ChatMessage>, callback: (EvaluationResult) -> Unit) -> Unit,
    onConfirmPass: (fullConversationSummary: String, minutes: Int, record: EvaluationResult) -> Unit,
    onDismiss: (fullConversationSummary: String, record: EvaluationResult?) -> Unit
) {
    var step by remember { mutableStateOf(OverlayStep.CONVERSATION_INQUIRY) }
    var inputText by remember { mutableStateOf("") }
    var currentResult by remember { mutableStateOf<EvaluationResult?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }

    // 多轮对话历史记录
    val conversationHistory = remember { mutableStateListOf<ChatMessage>() }
    val chatListState = rememberLazyListState()

    // 格式化对话历史为全量纪要字符串
    fun getFullConversationSummary(): String {
        if (conversationHistory.isEmpty()) return inputText.trim()
        return conversationHistory.joinToString(" ➔ ") { msg ->
            if (msg.sender == ChatSender.USER) "用户: ${msg.text}" else "AI追问: ${msg.text}"
        }
    }

    // 快捷常用理由模版
    val quickTemplates = listOf(
        "看收藏的教程视频",
        "查快递物流进度",
        "回复工作紧急消息",
        "买日用品卫生纸",
        "线上会议投屏演示"
    )

    LaunchedEffect(conversationHistory.size, isAiThinking) {
        val totalCount = conversationHistory.size + if (isAiThinking) 1 else 0
        if (totalCount > 0) {
            chatListState.animateScrollToItem(totalCount - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC1A1614)) // 半透明微暖深咖遮罩，保证对比度
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        // 主卡片 (纯净暖白 + 柔和阴影 + 1dp 细暖边框)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color(0x33000000)
                ),
            shape = RoundedCornerShape(24.dp),
            color = WarmSurface,
            border = BorderStroke(1.dp, WarmBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶栏：应用图标、目标名称、审查官称号与关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (targetAppIcon != null) {
                            val bitmap = remember(targetAppIcon) { targetAppIcon.toBitmap(80, 80) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(WarmSurfaceContainer, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isTest) "测试拦截【$targetAppName】" else "已拦截【$targetAppName】",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isTest) CaramelSecondary else if (isModelReady) SageGreen else AmberWarm,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTest) "演示测试 (不写入审批记录)" else if (isModelReady) "AI 意图审查引擎就绪" else "AI 引擎就绪中...",
                                    fontSize = 11.sp,
                                    color = if (isTest) CaramelSecondary else if (isModelReady) SageGreen else AmberWarm
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onDismiss(getFullConversationSummary(), currentResult) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(WarmSurfaceContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "取消",
                            tint = WarmTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 人格标签
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TerracottaPrimaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "审查官：$personaTitle",
                            fontSize = 11.sp,
                            color = TerracottaPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 核心状态分支切换
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "overlayStepTransition"
                ) { targetStep ->
                    when (targetStep) {
                        OverlayStep.CONVERSATION_INQUIRY -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 历史多轮气泡对话展示区
                                if (conversationHistory.isNotEmpty() || isAiThinking) {
                                    LazyColumn(
                                        state = chatListState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (conversationHistory.size > 2) 180.dp else 130.dp)
                                            .background(WarmSurfaceContainer, RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(conversationHistory) { msg ->
                                            ChatBubbleItem(message = msg)
                                        }
                                        if (isAiThinking) {
                                            item {
                                                TypingIndicatorBubble()
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                } else {
                                    Text(
                                        text = "请明确说明打开【$targetAppName】的具体目的（完成什么具体事情）",
                                        fontSize = 12.sp,
                                        color = WarmTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // 动态 AI 引导建议
                                val dynamicGuidance = currentResult?.guidanceTip?.takeIf { it.isNotBlank() }
                                if (!dynamicGuidance.isNullOrBlank() && conversationHistory.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = AmberWarmContainer,
                                        border = BorderStroke(0.8.dp, AmberWarm.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Psychology,
                                                contentDescription = null,
                                                tint = AmberWarm,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "💡 引导建议：$dynamicGuidance",
                                                fontSize = 11.sp,
                                                color = OnAmberWarmContainer,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // 理由输入框
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = {
                                        if (it.length <= 300) {
                                            inputText = it
                                        }
                                    },
                                    enabled = !isAiThinking,
                                    placeholder = {
                                        Text(
                                            text = if (isAiThinking) "AI 审查官正在逐字推演审阅中..."
                                            else if (!dynamicGuidance.isNullOrBlank()) dynamicGuidance
                                            else if (conversationHistory.isEmpty()) "例如：看收藏的Android教程 / 查快递 / 买日用品..."
                                            else "针对追问补充具体要做的事情...",
                                            fontSize = 13.sp,
                                            color = if (isAiThinking) TerracottaPrimary else WarmTextMuted
                                        )
                                    },
                                    supportingText = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = "${inputText.length}/300",
                                                fontSize = 10.sp,
                                                color = if (inputText.length >= 280) AmberWarm else WarmTextMuted
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(105.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TerracottaPrimary,
                                        unfocusedBorderColor = WarmBorder,
                                        focusedTextColor = WarmTextPrimary,
                                        unfocusedTextColor = WarmTextPrimary,
                                        focusedContainerColor = WarmSurface,
                                        unfocusedContainerColor = WarmSurface
                                    )
                                )

                                // 快捷模版选择
                                if (conversationHistory.isEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        quickTemplates.forEach { template ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = WarmSurfaceContainer,
                                                modifier = Modifier.clickable { inputText = template }
                                            ) {
                                                Text(
                                                    text = template,
                                                    fontSize = 11.sp,
                                                    color = WarmTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 操作按键
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (conversationHistory.isNotEmpty()) {
                                        Button(
                                            onClick = { onDismiss(getFullConversationSummary(), currentResult) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CoralDangerContainer,
                                                contentColor = CoralDanger
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                        ) {
                                            Text("放弃专注", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (inputText.trim().isNotBlank() && !isAiThinking) {
                                                val userMsg = inputText.trim()
                                                conversationHistory.add(ChatMessage(sender = ChatSender.USER, text = userMsg))
                                                inputText = ""
                                                isAiThinking = true

                                                onEvaluateConversation(conversationHistory.toList()) { result ->
                                                    isAiThinking = false
                                                    currentResult = result
                                                    when (result.decision) {
                                                        DecisionType.ALLOW -> {
                                                            conversationHistory.add(ChatMessage(sender = ChatSender.AI, text = result.comment))
                                                            step = OverlayStep.APPROVED_SELECT_TIME
                                                        }
                                                        DecisionType.RETRY -> {
                                                            conversationHistory.add(ChatMessage(sender = ChatSender.AI, text = result.comment))
                                                        }
                                                        DecisionType.DENY -> {
                                                            conversationHistory.add(ChatMessage(sender = ChatSender.AI, text = result.comment))
                                                            step = OverlayStep.REJECTED_FEEDBACK
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = inputText.trim().isNotBlank() && !isAiThinking,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TerracottaPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                    ) {
                                        if (isAiThinking) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("审阅中...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (conversationHistory.isEmpty()) "提交 AI 审核" else "发送补充说明",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OverlayStep.APPROVED_SELECT_TIME -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 审批通过标志与评语展示
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SageGreenContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = SageGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "审批结论：通过放行 (ALLOW)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSageGreenContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // AI 终审个性化评语卡片
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = WarmSurfaceContainer,
                                    border = BorderStroke(0.8.dp, WarmBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "审查官总结：",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TerracottaPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentResult?.comment ?: "已核准你的使用意图，请专注完成任务。",
                                            fontSize = 12.sp,
                                            color = WarmTextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "请旋转转盘选择本次专注放行时长",
                                    fontSize = 12.sp,
                                    color = WarmTextSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val aiMinutes = (currentResult?.suggestedMinutes ?: 15).coerceIn(1, 480)
                                DurationDialPicker(
                                    initialMinutes = aiMinutes,
                                    aiSuggestedMinutes = aiMinutes,
                                    onConfirm = { minutes ->
                                        currentResult?.let { record ->
                                            onConfirmPass(getFullConversationSummary(), minutes, record)
                                        }
                                    }
                                )
                            }
                        }

                        OverlayStep.REJECTED_FEEDBACK -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CoralDangerContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Block,
                                            contentDescription = null,
                                            tint = CoralDanger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "审批结论：予以拦截 (DENY)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnCoralDangerContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = WarmSurfaceContainer,
                                    border = BorderStroke(0.8.dp, CoralDanger.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "审查官裁决：",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralDanger
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentResult?.comment ?: "理由不符合正当使用规则，本次申请不予放行。",
                                            fontSize = 12.sp,
                                            color = WarmTextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            conversationHistory.clear()
                                            inputText = ""
                                            currentResult = null
                                            step = OverlayStep.CONVERSATION_INQUIRY
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        border = BorderStroke(1.dp, WarmBorder)
                                    ) {
                                        Text("重新陈述", fontSize = 12.sp, color = WarmTextPrimary)
                                    }

                                    Button(
                                        onClick = { onDismiss(getFullConversationSummary(), currentResult) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CoralDanger,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                    ) {
                                        Text("遵守自律", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * IM 风格单条气泡组件
 */
@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.sender == ChatSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(TerracottaPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) TerracottaPrimaryContainer else WarmSurface,
            border = if (!isUser) BorderStroke(0.8.dp, WarmBorder) else null
        ) {
            Text(
                text = message.text,
                fontSize = 12.sp,
                color = if (isUser) OnTerracottaContainer else WarmTextPrimary,
                fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 正在打字律动动效组件 (Typing Indicator)
 */
@Composable
fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(TerracottaPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Psychology,
                contentDescription = null,
                tint = TerracottaPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 2.dp, bottomEnd = 14.dp),
            color = WarmSurface,
            border = BorderStroke(0.8.dp, WarmBorder)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                BouncingDot(initialDelay = 0)
                BouncingDot(initialDelay = 160)
                BouncingDot(initialDelay = 320)
            }
        }
    }
}

@Composable
fun BouncingDot(initialDelay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotBouncing")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, delayMillis = initialDelay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, delayMillis = initialDelay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(6.dp)
            .background(TerracottaPrimary.copy(alpha = alpha), CircleShape)
    )
}
