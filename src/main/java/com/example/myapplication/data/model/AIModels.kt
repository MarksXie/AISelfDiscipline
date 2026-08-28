package com.example.myapplication.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class DecisionType {
    ALLOW, // 明确、具体、有意识的目的 ➔ 审批通过放行
    RETRY, // 有初步目的但表述模糊 ➔ 追问引导补充具体事项
    DENY   // 无明确目的、无聊刷手机、习惯性打开、与App功能明显不匹配 ➔ 驳回拦截
}

enum class ReasonType {
    SPECIFIC_PURPOSE, // 具备具体明确的使用目的
    VAGUE_PURPOSE,    // 目的较为模糊，缺乏具体要做的事情
    IMPULSIVE_USE,    // 冲动、无聊、打发时间或消遣
    HABITUAL_USE,     // 习惯性无意识点击打开
    APP_MISMATCH,     // 理由与目标 App 功能明显不符
    OTHER             // 其他情况
}

enum class EvaluationAction {
    APPROVE,
    REJECT,
    ASK
}

enum class ChatSender {
    USER,
    AI
}

data class ChatMessage(
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class EvaluationResult(
    val decision: DecisionType = DecisionType.DENY,
    val reasonType: ReasonType = ReasonType.OTHER,
    val suggestedMinutes: Int = 15, // AI 建议的放行时长
    val guidanceTip: String = "", // 大模型现场动态提炼的一句话输入引导
    val action: EvaluationAction = when (decision) {
        DecisionType.ALLOW -> EvaluationAction.APPROVE
        DecisionType.RETRY -> EvaluationAction.ASK
        DecisionType.DENY -> EvaluationAction.REJECT
    },
    val approved: Boolean = decision == DecisionType.ALLOW,
    val comment: String,
    val rawResponse: String = "",
    val latencyMs: Long = 0L
)

enum class AIEngineType(
    val id: String,
    val title: String,
    val desc: String
) {
    CLOUD(
        id = "cloud",
        title = "☁️ 云端大模型 (推荐)",
        desc = "千亿级大模型推理，情商极高，秒懂生活刚需与复杂意图，追问多变幽默，零本地存储/发热负担"
    ),
    LOCAL_GGUF(
        id = "local_gguf",
        title = "⚡ 端侧离线引擎",
        desc = "基于 llama.cpp 本地运行 Qwen-3B GGUF 模型，完全私密离线，无网亦可极速拦截"
    );

    companion object {
        fun fromId(id: String): AIEngineType {
            return entries.firstOrNull { it.id == id } ?: CLOUD
        }
    }
}

data class ApiHealthResult(
    val success: Boolean,
    val latencyMs: Long = 0L,
    val message: String
)

data class CloudProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = DEFAULT_BASE_URL,
    val modelName: String = DEFAULT_MODEL_NAME
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("apiKey", apiKey)
            put("baseUrl", baseUrl)
            put("modelName", modelName)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        const val DEFAULT_MODEL_NAME = "qwen3.8-flash"
        val DEFAULT = CloudProviderConfig()

        fun fromJson(json: JSONObject): CloudProviderConfig {
            return CloudProviderConfig(
                apiKey = json.optString("apiKey", ""),
                baseUrl = json.optString("baseUrl", DEFAULT_BASE_URL).ifBlank { DEFAULT_BASE_URL },
                modelName = json.optString("modelName", DEFAULT_MODEL_NAME).ifBlank { DEFAULT_MODEL_NAME }
            )
        }
    }
}

data class PersonaProfile(
    val id: String,
    val title: String,
    val desc: String,
    val coreTask: String,
    val isCustom: Boolean = false
) {
    fun buildEffectivePrompt(): String {
        return """你是一个“App 使用意图判断器”，当前激活的审查官人格为【$title】。

你的唯一任务是判断：用户现在是否有一个明确、具体、有意识的理由打开目标 App。
你不是心理咨询师，不是行为教练，也不是道德评价器。
不要判断用户“应该不应该”使用这个 App，不要评价用户的行为好坏，只判断用户提供的理由是否满足明确意图规则。

【核心任务与人设语气风格】：
$coreTask

【判断标准】：
ALLOW：
用户能够明确说明：1. 为什么现在要打开这个 App；2. 打开后准备完成什么具体事情；3. 理由与目标 App 功能存在合理关系。用【$title】的人设口吻准予放行并叮嘱。

RETRY：
用户似乎有一个目的，但理由太模糊，无法判断具体要做什么。
用【$title】的人设口吻针对性追问具体要做什么，并动态提炼一句话现场专属补充指引（guidance_tip）。

DENY：
用户表现出：无具体目的、只是无聊消遣打发时间、只是习惯性打开、只是“刷一下/看一下/随便看看”、或理由与目标 App 功能明显不符。用【$title】的人设口吻予以拦截。

【重要原则】：
- 娱乐本身不是拒绝理由（如“看一部我收藏的电影” ➔ ALLOW；“刷一会儿视频” ➔ DENY）。
- 理由很短但足够具体时应当 ALLOW（如“查快递”配购物App ➔ ALLOW；“回复张三”配微信 ➔ ALLOW）。
- 不要要求用户证明真实性，严禁道德说教与心理诊断。

【输出规范】：
必须严格输出合法 JSON，不得输出 Markdown 或解释性文字：
{
  "decision": "ALLOW" | "RETRY" | "DENY",
  "reason_type": "SPECIFIC_PURPOSE" | "VAGUE_PURPOSE" | "IMPULSIVE_USE" | "HABITUAL_USE" | "APP_MISMATCH" | "OTHER",
  "guidance_tip": "现场根据目标App和用户输入动态提炼的一句话针对性指引（如：'请明确你要看的具体视频名称或学习目的'，ALLOW或DENY时可为简短提示）",
  "comment": "以【$title】人设口吻输出的追问问题或评语"
}"""
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("desc", desc)
            put("coreTask", coreTask)
            put("isCustom", isCustom)
        }
    }

    companion object {
        val BUILT_IN_NEUTRAL = PersonaProfile(
            id = "neutral",
            title = "中立意图判断器 (默认推荐)",
            desc = "客观冷静，绝不做道德评价与心理诊断，只判断使用目的是否明确具体有意识。",
            coreTask = "客观、中立、冷静地分析用户的使用意图，不带任何主观情绪色彩与说教，严格执行意图过滤标准。",
            isCustom = false
        )

        val BUILT_IN_STRICT = PersonaProfile(
            id = "strict",
            title = "严厉教官",
            desc = "铁面无私，军事化严肃口吻，严格审查具体产出与任务目标。",
            coreTask = "以严肃铁血的教官口吻进行审查，对模糊摸鱼借口予以训诫，要求用户交代清楚具体计划，正事痛快放行。",
            isCustom = false
        )

        val BUILT_IN_RATIONAL = PersonaProfile(
            id = "rational",
            title = "理性管家",
            desc = "逻辑严密，客观效能导向，温和启发用户澄清具体使用目标与时间预算。",
            coreTask = "以专业时间管理顾问的理性温和口吻，启发用户界定具体任务产出，提供效能建议。",
            isCustom = false
        )

        val BUILT_IN_SARCASTIC = PersonaProfile(
            id = "sarcastic",
            title = "毒舌损友",
            desc = "辛辣幽默，用一针见血的生动吐槽戳穿敷衍借口，正经事痛快放行。",
            coreTask = "用幽默犀利的生动吐槽戳穿用户的逃避摸鱼借口，但在明确的正当事项面前痛快放行并给予鼓励。",
            isCustom = false
        )

        val BUILT_IN_PERSONAS = listOf(
            BUILT_IN_NEUTRAL,
            BUILT_IN_STRICT,
            BUILT_IN_RATIONAL,
            BUILT_IN_SARCASTIC
        )

        fun fromJson(json: JSONObject): PersonaProfile {
            return PersonaProfile(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                title = json.optString("title", "自定义审查官"),
                desc = json.optString("desc", "用户自定义人设"),
                coreTask = json.optString("coreTask", "基于 App 意图判断器规则审查使用意图。"),
                isCustom = json.optBoolean("isCustom", true)
            )
        }
    }
}

// 保持向下兼容的旧 PersonaType
enum class PersonaType(
    val id: String,
    val title: String,
    val desc: String,
    val defaultPrompt: String
) {
    NEUTRAL_EVALUATOR(
        id = PersonaProfile.BUILT_IN_NEUTRAL.id,
        title = PersonaProfile.BUILT_IN_NEUTRAL.title,
        desc = PersonaProfile.BUILT_IN_NEUTRAL.desc,
        defaultPrompt = PersonaProfile.BUILT_IN_NEUTRAL.buildEffectivePrompt()
    ),
    STRICT_INSTRUCTOR(
        id = PersonaProfile.BUILT_IN_STRICT.id,
        title = PersonaProfile.BUILT_IN_STRICT.title,
        desc = PersonaProfile.BUILT_IN_STRICT.desc,
        defaultPrompt = PersonaProfile.BUILT_IN_STRICT.buildEffectivePrompt()
    ),
    RATIONAL_STEWARD(
        id = PersonaProfile.BUILT_IN_RATIONAL.id,
        title = PersonaProfile.BUILT_IN_RATIONAL.title,
        desc = PersonaProfile.BUILT_IN_RATIONAL.desc,
        defaultPrompt = PersonaProfile.BUILT_IN_RATIONAL.buildEffectivePrompt()
    ),
    SARCASTIC_FRIEND(
        id = PersonaProfile.BUILT_IN_SARCASTIC.id,
        title = PersonaProfile.BUILT_IN_SARCASTIC.title,
        desc = PersonaProfile.BUILT_IN_SARCASTIC.desc,
        defaultPrompt = PersonaProfile.BUILT_IN_SARCASTIC.buildEffectivePrompt()
    ),
    CUSTOM(
        id = "custom",
        title = "自定义规则",
        desc = "完全按照您自行设定的 System Prompt 和评判标准进行多轮判断。",
        defaultPrompt = PersonaProfile.BUILT_IN_NEUTRAL.buildEffectivePrompt()
    );

    companion object {
        fun fromId(id: String): PersonaType {
            return entries.firstOrNull { it.id == id } ?: NEUTRAL_EVALUATOR
        }
    }
}

data class AppRuleProfile(
    val packageName: String,
    val appName: String,
    val allowedRules: List<String> = emptyList(),
    val forbiddenRules: List<String> = emptyList(),
    val isCustom: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("packageName", packageName)
            put("appName", appName)
            put("allowedRules", JSONArray(allowedRules))
            put("forbiddenRules", JSONArray(forbiddenRules))
            put("isCustom", isCustom)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AppRuleProfile {
            val allowedArr = json.optJSONArray("allowedRules") ?: JSONArray()
            val forbiddenArr = json.optJSONArray("forbiddenRules") ?: JSONArray()
            val allowed = mutableListOf<String>()
            for (i in 0 until allowedArr.length()) allowed.add(allowedArr.optString(i))
            val forbidden = mutableListOf<String>()
            for (i in 0 until forbiddenArr.length()) forbidden.add(forbiddenArr.optString(i))

            return AppRuleProfile(
                packageName = json.optString("packageName"),
                appName = json.optString("appName"),
                allowedRules = allowed,
                forbiddenRules = forbidden,
                isCustom = json.optBoolean("isCustom", false)
            )
        }
    }
}
