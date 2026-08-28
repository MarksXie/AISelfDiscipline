package com.example.myapplication.ai

import android.util.Log
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSender
import com.example.myapplication.data.model.DecisionType
import com.example.myapplication.data.model.EvaluationAction
import com.example.myapplication.data.model.EvaluationResult
import com.example.myapplication.data.model.ReasonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class LlamaCppEngine : AIEngine {

    override val engineName: String = "llama.cpp 端侧离线引擎 (Qwen-3B)"

    private var nativeModelHandle: Long = 0L
    private var isModelLoaded: Boolean = false

    companion object {
        private const val TAG = "LlamaCppEngine"

        // GBNF 语法强约束：严格限制生成标准意图判断结构
        const val GBNF_GRAMMAR = """
root        ::= "{" ws "\"decision\":" ws decision "," ws "\"reason_type\":" ws reason_type "," ws "\"guidance_tip\":" ws string "," ws "\"comment\":" ws string ws "}"
decision    ::= "\"ALLOW\"" | "\"RETRY\"" | "\"DENY\""
reason_type ::= "\"SPECIFIC_PURPOSE\"" | "\"VAGUE_PURPOSE\"" | "\"IMPULSIVE_USE\"" | "\"HABITUAL_USE\"" | "\"APP_MISMATCH\"" | "\"OTHER\""
string      ::= "\"" ([^"\\] | "\\" .)* "\""
ws          ::= [ \t\n]*
"""
    }

    override suspend fun preheat(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        if (!NativeLlamaBridge.isLibraryLoaded) {
            Log.w(TAG, "Native library libllama_jni.so not loaded.")
            return@withContext false
        }

        val file = File(modelPath)
        if (!file.exists()) {
            Log.w(TAG, "Model file does not exist at $modelPath")
            return@withContext false
        }

        try {
            if (nativeModelHandle != 0L) {
                NativeLlamaBridge.nativeFree(nativeModelHandle)
                nativeModelHandle = 0L
            }
            nativeModelHandle = NativeLlamaBridge.nativeLoadModel(
                modelPath = modelPath,
                nThreads = 6,
                nGpuLayers = 99,
                useNpu = true
            )
            isModelLoaded = nativeModelHandle != 0L
            isModelLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model via JNI", e)
            false
        }
    }

    override suspend fun evaluateConversation(
        conversationHistory: List<ChatMessage>,
        targetAppName: String,
        systemPrompt: String
    ): EvaluationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (!NativeLlamaBridge.isLibraryLoaded || nativeModelHandle == 0L) {
            val lastUserMsg = conversationHistory.lastOrNull { it.sender == ChatSender.USER }?.text ?: ""
            return@withContext performFallbackEvaluation(lastUserMsg, targetAppName, conversationHistory.size)
        }

        val dynamicAppContext = AppIntentContextHelper.buildDynamicContextPrompt(targetAppName)

        val promptBuilder = StringBuilder()
        promptBuilder.append("<|im_start|>system\n")
            .append(systemPrompt).append("\n\n")
            .append(dynamicAppContext).append("\n<|im_end|>\n")
        conversationHistory.forEachIndexed { index, msg ->
            if (index == 0) {
                promptBuilder.append("<|im_start|>user\n目标App：").append(targetAppName)
                    .append("\n理由：").append(msg.text).append("\n<|im_end|>\n")
            } else if (msg.sender == ChatSender.USER) {
                promptBuilder.append("<|im_start|>user\n").append(msg.text).append("\n<|im_end|>\n")
            } else {
                promptBuilder.append("<|im_start|>assistant\n").append(msg.text).append("\n<|im_end|>\n")
            }
        }
        promptBuilder.append("<|im_start|>assistant\n")

        val formattedPrompt = promptBuilder.toString()

        try {
            val jsonOutput = NativeLlamaBridge.nativeEvaluate(
                handle = nativeModelHandle,
                prompt = formattedPrompt,
                grammar = GBNF_GRAMMAR
            )
            val latency = System.currentTimeMillis() - startTime

            val json = JSONObject(jsonOutput)
            val decisionStr = json.optString("decision", json.optString("action", ""))
            val reasonTypeStr = json.optString("reason_type", "")
            val guidanceTip = json.optString("guidance_tip", "")
            val comment = json.optString("comment", "评估完成")

            val decision = when (decisionStr.uppercase()) {
                "ALLOW", "APPROVE" -> DecisionType.ALLOW
                "RETRY", "ASK" -> DecisionType.RETRY
                "DENY", "REJECT" -> DecisionType.DENY
                else -> if (json.optBoolean("approved", false)) DecisionType.ALLOW else DecisionType.DENY
            }

            val reasonType = when (reasonTypeStr.uppercase()) {
                "SPECIFIC_PURPOSE" -> ReasonType.SPECIFIC_PURPOSE
                "VAGUE_PURPOSE" -> ReasonType.VAGUE_PURPOSE
                "IMPULSIVE_USE" -> ReasonType.IMPULSIVE_USE
                "HABITUAL_USE" -> ReasonType.HABITUAL_USE
                "APP_MISMATCH" -> ReasonType.APP_MISMATCH
                else -> if (decision == DecisionType.ALLOW) ReasonType.SPECIFIC_PURPOSE else ReasonType.OTHER
            }

            val suggestedMinutes = json.optInt("suggested_minutes", json.optInt("suggestedMinutes", 15)).coerceIn(1, 480)

            EvaluationResult(
                decision = decision,
                reasonType = reasonType,
                suggestedMinutes = suggestedMinutes,
                guidanceTip = guidanceTip,
                comment = comment,
                rawResponse = jsonOutput,
                latencyMs = latency
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in JNI multi-turn inference", e)
            val lastUserMsg = conversationHistory.lastOrNull { it.sender == ChatSender.USER }?.text ?: ""
            performFallbackEvaluation(lastUserMsg, targetAppName, conversationHistory.size)
        }
    }

    override suspend fun evaluateReason(
        reason: String,
        targetAppName: String,
        systemPrompt: String
    ): EvaluationResult {
        return evaluateConversation(
            conversationHistory = listOf(ChatMessage(sender = ChatSender.USER, text = reason)),
            targetAppName = targetAppName,
            systemPrompt = systemPrompt
        )
    }

    private fun performFallbackEvaluation(
        reason: String,
        targetAppName: String,
        historySize: Int
    ): EvaluationResult {
        val trimmed = reason.trim()
        val isValid = listOf(
            "开会", "会议", "工作", "客户", "学习", "考研", "紧急", "验证码",
            "纸", "卫生", "生活用品", "日用品", "买菜", "吃饭", "外卖", "买药", "看病", "药品",
            "刚需", "计划内", "开销", "生活开销", "个人卫生", "水电", "打车", "出行", "快递", "查快递",
            "收藏的", "指定", "回复", "发消息"
        ).any { trimmed.contains(it) }

        val decision: DecisionType
        val reasonType: ReasonType
        val comment: String

        if (isValid) {
            decision = DecisionType.ALLOW
            reasonType = ReasonType.SPECIFIC_PURPOSE
            comment = "检测到明确具体的使用意图，准予放行【$targetAppName】，请按计划专注使用。"
        } else if (trimmed.contains("无聊") || trimmed.contains("随便") || trimmed.contains("刷") || trimmed.contains("消遣")) {
            decision = DecisionType.DENY
            reasonType = ReasonType.IMPULSIVE_USE
            comment = "检测到无目的消遣与冲动使用，请明确具体要做的事情后再申请。"
        } else if (historySize <= 2) {
            decision = DecisionType.RETRY
            reasonType = ReasonType.VAGUE_PURPOSE
            comment = "使用目的较为宽泛，请具体说明你准备在【$targetAppName】中完成什么事情。"
        } else {
            decision = DecisionType.DENY
            reasonType = ReasonType.VAGUE_PURPOSE
            comment = "多次说明仍未给出具体目的，判定为无明确意图使用，已拦截。"
        }

        return EvaluationResult(
            decision = decision,
            reasonType = reasonType,
            comment = comment,
            rawResponse = "{\"decision\": \"$decision\", \"reason_type\": \"$reasonType\", \"comment\": \"$comment\"}",
            latencyMs = 150L
        )
    }

    override fun release() {
        if (nativeModelHandle != 0L && NativeLlamaBridge.isLibraryLoaded) {
            try {
                NativeLlamaBridge.nativeFree(nativeModelHandle)
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing model handle", e)
            }
            nativeModelHandle = 0L
        }
        isModelLoaded = false
    }

    override fun isReady(): Boolean {
        return NativeLlamaBridge.isLibraryLoaded && isModelLoaded
    }
}
