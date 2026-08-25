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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudOpenAIEngine(
    var apiKey: String = "",
    var baseUrl: String = "https://api.deepseek.com",
    var modelName: String = "deepseek-chat",
    var enableThinking: Boolean = false,
    var thinkingParamKey: String = "enable_thinking"
) : AIEngine {

    override val engineName: String
        get() = "云端大模型 ($modelName)"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "CloudOpenAIEngine"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun preheat(modelPath: String): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun evaluateConversation(
        conversationHistory: List<ChatMessage>,
        targetAppName: String,
        systemPrompt: String
    ): EvaluationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (apiKey.isBlank()) {
            return@withContext EvaluationResult(
                decision = DecisionType.DENY,
                reasonType = ReasonType.OTHER,
                comment = "云端 API Key 未配置，请前往【AI 审查官设置】填写 API Key，或切换为端侧离线引擎。",
                rawResponse = "{\"error\": \"API Key is empty\"}",
                latencyMs = 0L
            )
        }

        val requestUrl = resolveChatCompletionsUrl(baseUrl)

        // 获取动态 App 上下文
        val dynamicAppContext = AppIntentContextHelper.buildDynamicContextPrompt(targetAppName)

        // 组装 OpenAI 标准格式 messages 列表
        val messagesArray = JSONArray()

        val fullSystemPrompt = """
$systemPrompt

$dynamicAppContext

【输出必须严格遵循以下 JSON Schema】：
{
  "decision": "ALLOW" | "RETRY" | "DENY",
  "reason_type": "SPECIFIC_PURPOSE" | "VAGUE_PURPOSE" | "IMPULSIVE_USE" | "HABITUAL_USE" | "APP_MISMATCH" | "OTHER",
  "guidance_tip": "若为RETRY时针对性提炼的一句话引导补充建议（如'请具体说明要买什么物品'），若ALLOW或DENY可留空",
  "comment": "简明扼要的说明或追问提问"
}
""".trimIndent()

        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", fullSystemPrompt)
        })

        // 组装多轮历史（完整无截断）
        conversationHistory.forEachIndexed { index, msg ->
            val role = if (msg.sender == ChatSender.USER) "user" else "assistant"
            val content = if (index == 0 && msg.sender == ChatSender.USER) {
                "目标App：$targetAppName\n理由：${msg.text}"
            } else {
                msg.text
            }
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", content)
            })
        }

        val isQwenOrDashScope = baseUrl.contains("dashscope", ignoreCase = true) ||
                baseUrl.contains("aliyuncs", ignoreCase = true) ||
                thinkingParamKey.trim() == "enable_thinking"

        val requestJson = JSONObject().apply {
            put("model", modelName.trim())
            put("messages", messagesArray)
            put("temperature", 0.6)
            put("response_format", JSONObject().put("type", "json_object"))
            if (enableThinking) {
                val key = thinkingParamKey.trim().ifBlank { "enable_thinking" }
                put(key, true)
                if (isQwenOrDashScope) {
                    put("preserve_thinking", false)
                }
            } else {
                if (isQwenOrDashScope) {
                    put("enable_thinking", false)
                    put("reasoning_effort", "none")
                }
            }
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val responseBodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Cloud API call failed with code: ${response.code}, body: $responseBodyStr")
                    val errorDetail = parseErrorMessage(responseBodyStr, response.code)
                    return@withContext EvaluationResult(
                        decision = DecisionType.DENY,
                        reasonType = ReasonType.OTHER,
                        comment = "云端 API 请求异常 (${response.code})：$errorDetail",
                        rawResponse = responseBodyStr,
                        latencyMs = latency
                    )
                }

                val responseJson = JSONObject(responseBodyStr)
                val choices = responseJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext EvaluationResult(
                        decision = DecisionType.DENY,
                        reasonType = ReasonType.OTHER,
                        comment = "云端大模型返回内容为空，请检查模型名称是否正确。",
                        rawResponse = responseBodyStr,
                        latencyMs = latency
                    )
                }

                val messageObj = choices.getJSONObject(0).optJSONObject("message")
                val contentStr = messageObj?.optString("content") ?: ""
                val cleanJsonStr = extractJsonString(contentStr)

                val resultJson = JSONObject(cleanJsonStr)
                val decisionStr = resultJson.optString("decision", resultJson.optString("action", ""))
                val reasonTypeStr = resultJson.optString("reason_type", "")
                val guidanceTip = resultJson.optString("guidance_tip", "")
                val comment = resultJson.optString("comment", "评估完成")

                val decision = when (decisionStr.uppercase()) {
                    "ALLOW", "APPROVE" -> DecisionType.ALLOW
                    "RETRY", "ASK" -> DecisionType.RETRY
                    "DENY", "REJECT" -> DecisionType.DENY
                    else -> if (resultJson.optBoolean("approved", false)) DecisionType.ALLOW else DecisionType.DENY
                }

                val reasonType = when (reasonTypeStr.uppercase()) {
                    "SPECIFIC_PURPOSE" -> ReasonType.SPECIFIC_PURPOSE
                    "VAGUE_PURPOSE" -> ReasonType.VAGUE_PURPOSE
                    "IMPULSIVE_USE" -> ReasonType.IMPULSIVE_USE
                    "HABITUAL_USE" -> ReasonType.HABITUAL_USE
                    "APP_MISMATCH" -> ReasonType.APP_MISMATCH
                    else -> if (decision == DecisionType.ALLOW) ReasonType.SPECIFIC_PURPOSE else ReasonType.OTHER
                }

                EvaluationResult(
                    decision = decision,
                    reasonType = reasonType,
                    guidanceTip = guidanceTip,
                    comment = comment,
                    rawResponse = cleanJsonStr,
                    latencyMs = latency
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Cloud API network execution exception", e)
            EvaluationResult(
                decision = DecisionType.DENY,
                reasonType = ReasonType.OTHER,
                comment = "连接云端 API 失败：${e.localizedMessage ?: "网络超时"}，请检查网络连接或 API Base URL 设置。",
                rawResponse = "{\"error\": \"${e.message}\"}",
                latencyMs = latency
            )
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

    override fun release() {
        // OkHttpClient 由连接池管理
    }

    override fun isReady(): Boolean {
        return apiKey.isNotBlank()
    }

    private fun resolveChatCompletionsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
    }

    private fun extractJsonString(raw: String): String {
        val trimmed = raw.trim()
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        return if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            trimmed.substring(jsonStart, jsonEnd + 1)
        } else {
            trimmed
        }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            val msg = errorObj?.optString("message") ?: json.optString("message", "")
            if (msg.isNotBlank()) msg else "HTTP $code 错误"
        } catch (e: Exception) {
            when (code) {
                401 -> "API Key 无效或未授权"
                404 -> "模型名称或 API 路径不存在"
                429 -> "请求配额不足或触发频率限制"
                500 -> "服务商服务器内部错误"
                else -> "HTTP $code 响应异常"
            }
        }
    }
}
