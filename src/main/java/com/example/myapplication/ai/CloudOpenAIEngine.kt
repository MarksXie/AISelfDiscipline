package com.example.myapplication.ai

import android.util.Log
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSender
import com.example.myapplication.data.model.DecisionType
import com.example.myapplication.data.model.EvaluationAction
import com.example.myapplication.data.model.EvaluationResult
import com.example.myapplication.data.model.ReasonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var modelName: String = "deepseek-chat"
) : AIEngine {

    override val engineName: String
        get() = "云端大模型 ($modelName)"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
  "suggested_minutes": 整数（若为ALLOW，必须根据用户具体任务性质、规模与耗时自主推理计算得出合理的放行分钟数，如2、5、8、13、22、35、50等任意合理整数，严禁机械地固定套用15或30；若为RETRY或DENY填0）,
  "guidance_tip": "若为RETRY时针对性提炼的一句话引导补充建议（如'请具体说明要买什么物品'），若ALLOW或DENY可留空",
  "comment": "简短精炼的说明或追问（严禁套话废话，必须在50字以内）"
}

【核心限制与推理法则】：
1. 评语字数：comment 必须短小精炼、一针见血，严格控制在 50 字以内。
2. 建议时长推理：suggested_minutes 必须由你根据任务难度与实际所需时长深度推理计算得出，禁止无脑返回固定值 15 或 30。
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

        val requestJson = JSONObject().apply {
            put("model", modelName.trim())
            put("messages", messagesArray)
            put("enable_thinking", false)
            put("preserve_thinking", false)
            put("max_completion_tokens", 500)
            put("temperature", 0.3)
            put("stream", false)
            put("response_format", JSONObject().put("type", "json_object"))
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        for (attempt in 0..1) {
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
                    val suggestedMinutes = resultJson.optInt("suggested_minutes", resultJson.optInt("suggestedMinutes", 15)).coerceIn(1, 480)

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

                    return@withContext EvaluationResult(
                        decision = decision,
                        reasonType = reasonType,
                        suggestedMinutes = suggestedMinutes,
                        guidanceTip = guidanceTip,
                        comment = comment,
                        rawResponse = cleanJsonStr,
                        latencyMs = latency
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud API evaluateConversation attempt $attempt failed: ${e.message}")
                if (attempt < 1) {
                    delay(1000)
                } else {
                    val latency = System.currentTimeMillis() - startTime
                    Log.e(TAG, "Cloud API network execution exception", e)
                    return@withContext EvaluationResult(
                        decision = DecisionType.DENY,
                        reasonType = ReasonType.OTHER,
                        comment = "连接云端 API 失败：${e.localizedMessage ?: "网络超时"}，请检查网络连接或 API Base URL 设置。",
                        rawResponse = "{\"error\": \"${e.message}\"}",
                        latencyMs = latency
                    )
                }
            }
        }

        EvaluationResult(
            decision = DecisionType.DENY,
            reasonType = ReasonType.OTHER,
            comment = "请求未完成",
            rawResponse = "{}",
            latencyMs = 0L
        )
    }

    /**
     * 生成长文本统计分析复盘报告（开启思考模式，字数控制在 800 字内）
     */
    suspend fun generateLongReport(
        userPrompt: String,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "云端 API Key 未配置，请前往【自律统计引擎设置】填写 API Key。"
        }

        val requestUrl = resolveChatCompletionsUrl(baseUrl)
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", modelName.trim())
            put("messages", messagesArray)
            put("enable_thinking", true)
            put("preserve_thinking", true)
            put("temperature", 0.7)
            put("stream", false)
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        for (attempt in 0..1) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBodyStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errorDetail = parseErrorMessage(responseBodyStr, response.code)
                        return@withContext "云端 API 请求异常 (${response.code})：$errorDetail"
                    }

                    val responseJson = JSONObject(responseBodyStr)
                    val choices = responseJson.optJSONArray("choices")
                    if (choices == null || choices.length() == 0) {
                        return@withContext "云端大模型返回内容为空，请检查模型名称与网络配置。"
                    }

                    val messageObj = choices.getJSONObject(0).optJSONObject("message")
                    val contentStr = messageObj?.optString("content") ?: ""
                    if (contentStr.isNotBlank()) {
                        return@withContext contentStr
                    } else {
                        val reasoningContent = messageObj?.optString("reasoning_content") ?: ""
                        if (reasoningContent.isNotBlank()) {
                            return@withContext reasoningContent
                        } else {
                            return@withContext "云端大模型返回内容为空，请检查模型配置与网络连接。"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud API generateLongReport attempt $attempt failed: ${e.message}")
                if (attempt < 1) {
                    delay(1000)
                } else {
                    Log.e(TAG, "Cloud API generateLongReport exception", e)
                    return@withContext "生成报告失败：${e.localizedMessage ?: "网络超时"}，请检查网络连接或 API 设置。"
                }
            }
        }
        "生成报告失败：重试后依然超时，请检查网络连接。"
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
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```json").removePrefix("```").trim()
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```").trim()
            }
        }
        val jsonStart = clean.indexOf('{')
        val jsonEnd = clean.lastIndexOf('}')
        return if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            clean.substring(jsonStart, jsonEnd + 1)
        } else {
            clean
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

    suspend fun testConnection(
        testApiKey: String = apiKey,
        testBaseUrl: String = baseUrl,
        testModelName: String = modelName
    ): com.example.myapplication.data.model.ApiHealthResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = testApiKey.trim()
        val url = testBaseUrl.trim()
        val model = testModelName.trim()

        if (key.isBlank()) {
            return@withContext com.example.myapplication.data.model.ApiHealthResult(
                success = false,
                latencyMs = 0L,
                message = "API Key 不能为空"
            )
        }

        val requestUrl = resolveChatCompletionsUrl(url)

        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "hi")
                })
            }

            val requestJson = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", 1)
            }

            val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val quickClient = httpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()

            quickClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val code = response.code
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    com.example.myapplication.data.model.ApiHealthResult(
                        success = true,
                        latencyMs = latency,
                        message = "连通正常 · 延迟 ${latency}ms"
                    )
                } else {
                    val errMsg = parseErrorMessage(body, code)
                    val friendlyMsg = when (code) {
                        401 -> "API Key 错误或已过期 (401)"
                        404 -> "模型 [$model] 不存在或端点错误 (404)"
                        429 -> "账户余额不足或超出频率限制 (429)"
                        else -> "HTTP $code: $errMsg"
                    }
                    com.example.myapplication.data.model.ApiHealthResult(
                        success = false,
                        latencyMs = latency,
                        message = friendlyMsg
                    )
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            val latency = System.currentTimeMillis() - start
            com.example.myapplication.data.model.ApiHealthResult(
                success = false,
                latencyMs = latency,
                message = "连接超时，请检查端点 URL 或网络"
            )
        } catch (e: java.net.UnknownHostException) {
            com.example.myapplication.data.model.ApiHealthResult(
                success = false,
                latencyMs = 0L,
                message = "域名解析失败，请检查 Base URL"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            com.example.myapplication.data.model.ApiHealthResult(
                success = false,
                latencyMs = latency,
                message = "连接失败: ${e.message ?: "未知异常"}"
            )
        }
    }
}
