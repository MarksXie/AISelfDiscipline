package com.example.myapplication.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.ai.AppIntentContextHelper
import com.example.myapplication.data.model.AIEngineType
import com.example.myapplication.data.model.AppInfo
import com.example.myapplication.data.model.AppRuleProfile
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.CloudProviderConfig
import com.example.myapplication.data.model.PersonaProfile
import com.example.myapplication.data.model.PersonaType
import com.example.myapplication.data.model.StatsReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_guard_preferences")

class AppLockRepository(private val context: Context) {

    companion object {
        private val KEY_BLACKLIST = stringSetPreferencesKey("blocked_packages")
        private val KEY_PERSONA = stringPreferencesKey("ai_persona")
        private val KEY_CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
        private val KEY_CUSTOM_PERSONAS_JSON = stringPreferencesKey("custom_personas_json")
        private val KEY_MODEL_PATH = stringPreferencesKey("model_path")
        private val KEY_PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        private val KEY_HISTORY_JSON = stringPreferencesKey("history_records_json")
        private val KEY_APP_RULES_JSON = stringPreferencesKey("app_rules_json")

        // 审查官云端大模型配置键（正式与测试环境物理隔离）
        private val KEY_ENGINE_TYPE = stringPreferencesKey("ai_engine_type")
        private val KEY_CLOUD_CONFIG_JSON = stringPreferencesKey("cloud_config_json")
        private val KEY_TEST_CLOUD_CONFIG_JSON = stringPreferencesKey("test_cloud_config_json")

        // 统计专属独立 AI 引擎配置键（正式与测试环境物理隔离）
        private val KEY_STATS_ENGINE_TYPE = stringPreferencesKey("stats_engine_type")
        private val KEY_STATS_CLOUD_CONFIG_JSON = stringPreferencesKey("stats_cloud_config_json")
        private val KEY_TEST_STATS_CLOUD_CONFIG_JSON = stringPreferencesKey("test_stats_cloud_config_json")

        // 统计报告持久化缓存键 (JSON Map: periodKey -> StatsReport JSON)
        private val KEY_STATS_REPORTS_CACHE_JSON = stringPreferencesKey("stats_reports_cache_json")

        // 审批明细前端隐藏过滤时间戳 (0 表示不隐藏，>0 仅展示此时间戳之后的记录)
        private val KEY_HISTORY_CLEAR_TIMESTAMP = stringPreferencesKey("history_clear_timestamp")

        // 开发者测试模式开关
        private val KEY_TEST_MODE_ENABLED = booleanPreferencesKey("test_mode_enabled")
    }

    val isProtectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROTECTION_ENABLED] ?: true
    }

    val isTestModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TEST_MODE_ENABLED] ?: false
    }

    val historyClearTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_HISTORY_CLEAR_TIMESTAMP]?.toLongOrNull() ?: 0L
    }

    val blacklistedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLACKLIST] ?: emptySet()
    }

    // 全部可用审查官人格列表（4个内置 + 用户自定义）
    val allPersonas: Flow<List<PersonaProfile>> = context.dataStore.data.map { prefs ->
        val customList = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]")
        PersonaProfile.BUILT_IN_PERSONAS + customList
    }

    // 当前激活的生效审查官
    val activePersona: Flow<PersonaProfile> = context.dataStore.data.map { prefs ->
        val activeId = prefs[KEY_PERSONA] ?: PersonaProfile.BUILT_IN_NEUTRAL.id
        val customList = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]")
        val all = PersonaProfile.BUILT_IN_PERSONAS + customList
        all.firstOrNull { it.id == activeId } ?: PersonaProfile.BUILT_IN_NEUTRAL
    }

    // 兼容旧接口
    val currentPersona: Flow<PersonaType> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_PERSONA] ?: PersonaType.NEUTRAL_EVALUATOR.id
        PersonaType.fromId(id)
    }

    val customPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_PROMPT] ?: PersonaType.CUSTOM.defaultPrompt
    }

    val modelPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_PATH] ?: "/sdcard/Download/qwen2.5-3b-instruct-q4_k_m.gguf"
    }

    // 审批引擎模式：默认 CLOUD
    val engineType: Flow<AIEngineType> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ENGINE_TYPE] ?: AIEngineType.CLOUD.id
        AIEngineType.fromId(id)
    }

    // 审查官正式环境云端配置
    val productionCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_CLOUD_CONFIG_JSON]
        if (jsonStr.isNullOrBlank()) {
            CloudProviderConfig.DEFAULT
        } else {
            try {
                CloudProviderConfig.fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                CloudProviderConfig.DEFAULT
            }
        }
    }

    // 审查官测试环境云端配置
    val testCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_TEST_CLOUD_CONFIG_JSON]
        if (jsonStr.isNullOrBlank()) {
            CloudProviderConfig.DEFAULT
        } else {
            try {
                CloudProviderConfig.fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                CloudProviderConfig.DEFAULT
            }
        }
    }

    // 审查官当前根据测试模式动态生效的云端配置
    val activeCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val isTest = prefs[KEY_TEST_MODE_ENABLED] ?: false
        val jsonKey = if (isTest) KEY_TEST_CLOUD_CONFIG_JSON else KEY_CLOUD_CONFIG_JSON
        val jsonStr = prefs[jsonKey]
        if (jsonStr.isNullOrBlank()) {
            CloudProviderConfig.DEFAULT
        } else {
            try {
                CloudProviderConfig.fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                CloudProviderConfig.DEFAULT
            }
        }
    }

    val cloudApiKey: Flow<String> = activeCloudConfig.map { it.apiKey }
    val cloudBaseUrl: Flow<String> = activeCloudConfig.map { it.baseUrl }
    val cloudModelName: Flow<String> = activeCloudConfig.map { it.modelName }

    // ==================== 统计专属独立 AI 引擎 Flows ====================
    val statsEngineType: Flow<AIEngineType> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_STATS_ENGINE_TYPE] ?: AIEngineType.CLOUD.id
        AIEngineType.fromId(id)
    }

    // 统计正式环境云端配置（专供 WorkManager 定时报告）
    val productionStatsCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_STATS_CLOUD_CONFIG_JSON]
        if (jsonStr.isNullOrBlank()) {
            CloudProviderConfig.DEFAULT
        } else {
            try {
                CloudProviderConfig.fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                CloudProviderConfig.DEFAULT
            }
        }
    }

    // 统计测试环境云端配置
    val testStatsCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_TEST_STATS_CLOUD_CONFIG_JSON]
        if (jsonStr.isNullOrBlank()) {
            CloudProviderConfig.DEFAULT
        } else {
            try {
                CloudProviderConfig.fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                CloudProviderConfig.DEFAULT
            }
        }
    }

    // 统计当前根据测试模式动态生效的云端配置
    val statsActiveCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val isTest = prefs[KEY_TEST_MODE_ENABLED] ?: false
        val jsonKey = if (isTest) KEY_TEST_STATS_CLOUD_CONFIG_JSON else KEY_STATS_CLOUD_CONFIG_JSON
        val jsonStr = prefs[jsonKey]
        if (jsonStr.isNullOrBlank()) {
            CloudProviderConfig.DEFAULT
        } else {
            try {
                CloudProviderConfig.fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                CloudProviderConfig.DEFAULT
            }
        }
    }

    // 统计报告缓存字典 Flow
    val statsReportsCache: Flow<Map<String, StatsReport>> = context.dataStore.data.map { prefs ->
        parseStatsReportsCacheJson(prefs[KEY_STATS_REPORTS_CACHE_JSON] ?: "{}")
    }

    val historyRecords: Flow<List<ApprovalRecord>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_HISTORY_JSON] ?: "[]"
        parseHistoryJson(jsonStr)
    }

    // 各 App 专属规则字典
    val appRulesMap: Flow<Map<String, AppRuleProfile>> = context.dataStore.data.map { prefs ->
        parseAppRulesMap(prefs[KEY_APP_RULES_JSON] ?: "{}")
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROTECTION_ENABLED] = enabled
        }
    }

    suspend fun addBlacklistedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST]?.toMutableSet() ?: mutableSetOf()
            current.add(packageName)
            prefs[KEY_BLACKLIST] = current
        }
    }

    suspend fun removeBlacklistedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST]?.toMutableSet() ?: mutableSetOf()
            current.remove(packageName)
            prefs[KEY_BLACKLIST] = current
        }
    }

    suspend fun toggleBlacklist(packageName: String, shouldBlock: Boolean) {
        if (shouldBlock) {
            addBlacklistedPackage(packageName)
        } else {
            removeBlacklistedPackage(packageName)
        }
    }

    suspend fun setAllBlacklist(packages: List<String>, shouldBlock: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST]?.toMutableSet() ?: mutableSetOf()
            if (shouldBlock) {
                current.addAll(packages)
            } else {
                current.removeAll(packages.toSet())
            }
            prefs[KEY_BLACKLIST] = current
        }
    }

    suspend fun savePersona(persona: PersonaProfile) {
        context.dataStore.edit { prefs ->
            val list = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]").toMutableList()
            val existingIndex = list.indexOfFirst { it.id == persona.id }
            if (existingIndex >= 0) {
                list[existingIndex] = persona
            } else {
                list.add(persona)
            }
            prefs[KEY_CUSTOM_PERSONAS_JSON] = serializeCustomPersonasJson(list)
        }
    }

    suspend fun saveCustomPersona(profile: PersonaProfile) {
        savePersona(profile)
    }

    suspend fun setActivePersona(personaId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSONA] = personaId
        }
    }

    suspend fun setActivePersonaId(personaId: String) {
        setActivePersona(personaId)
    }

    suspend fun deleteCustomPersona(personaId: String) {
        context.dataStore.edit { prefs ->
            val list = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]").toMutableList()
            list.removeAll { it.id == personaId }
            prefs[KEY_CUSTOM_PERSONAS_JSON] = serializeCustomPersonasJson(list)

            val activeId = prefs[KEY_PERSONA]
            if (activeId == personaId) {
                prefs[KEY_PERSONA] = PersonaProfile.BUILT_IN_NEUTRAL.id
            }
        }
    }

    suspend fun setEngineType(engineType: AIEngineType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENGINE_TYPE] = engineType.id
        }
    }

    suspend fun saveCloudConfig(
        apiKey: String,
        baseUrl: String,
        modelName: String
    ) {
        context.dataStore.edit { prefs ->
            val config = CloudProviderConfig(
                apiKey = apiKey.trim(),
                baseUrl = baseUrl.trim().ifBlank { CloudProviderConfig.DEFAULT_BASE_URL },
                modelName = modelName.trim().ifBlank { CloudProviderConfig.DEFAULT_MODEL_NAME }
            )
            prefs[KEY_CLOUD_CONFIG_JSON] = config.toJson().toString()
        }
    }

    suspend fun saveTestCloudConfig(
        apiKey: String,
        baseUrl: String,
        modelName: String
    ) {
        context.dataStore.edit { prefs ->
            val config = CloudProviderConfig(
                apiKey = apiKey.trim(),
                baseUrl = baseUrl.trim().ifBlank { CloudProviderConfig.DEFAULT_BASE_URL },
                modelName = modelName.trim().ifBlank { CloudProviderConfig.DEFAULT_MODEL_NAME }
            )
            prefs[KEY_TEST_CLOUD_CONFIG_JSON] = config.toJson().toString()
        }
    }

    suspend fun setCustomPrompt(prompt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_PROMPT] = prompt
        }
    }

    suspend fun setModelPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODEL_PATH] = path
        }
    }

    suspend fun setTestModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEST_MODE_ENABLED] = enabled
        }
    }

    suspend fun setStatsEngineType(engineType: AIEngineType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STATS_ENGINE_TYPE] = engineType.id
        }
    }

    suspend fun saveStatsCloudConfig(
        apiKey: String,
        baseUrl: String,
        modelName: String
    ) {
        context.dataStore.edit { prefs ->
            val config = CloudProviderConfig(
                apiKey = apiKey.trim(),
                baseUrl = baseUrl.trim().ifBlank { CloudProviderConfig.DEFAULT_BASE_URL },
                modelName = modelName.trim().ifBlank { CloudProviderConfig.DEFAULT_MODEL_NAME }
            )
            prefs[KEY_STATS_CLOUD_CONFIG_JSON] = config.toJson().toString()
        }
    }

    suspend fun saveTestStatsCloudConfig(
        apiKey: String,
        baseUrl: String,
        modelName: String
    ) {
        context.dataStore.edit { prefs ->
            val config = CloudProviderConfig(
                apiKey = apiKey.trim(),
                baseUrl = baseUrl.trim().ifBlank { CloudProviderConfig.DEFAULT_BASE_URL },
                modelName = modelName.trim().ifBlank { CloudProviderConfig.DEFAULT_MODEL_NAME }
            )
            prefs[KEY_TEST_STATS_CLOUD_CONFIG_JSON] = config.toJson().toString()
        }
    }

    suspend fun saveStatsReport(report: StatsReport) {
        context.dataStore.edit { prefs ->
            val map = parseStatsReportsCacheJson(prefs[KEY_STATS_REPORTS_CACHE_JSON] ?: "{}").toMutableMap()
            map[report.periodKey] = report
            prefs[KEY_STATS_REPORTS_CACHE_JSON] = serializeStatsReportsCacheJson(map)
        }
    }

    suspend fun getStatsReport(periodKey: String): StatsReport? {
        val prefs = context.dataStore.data.first()
        val map = parseStatsReportsCacheJson(prefs[KEY_STATS_REPORTS_CACHE_JSON] ?: "{}")
        return map[periodKey]
    }

    suspend fun addHistoryRecord(record: ApprovalRecord) {
        context.dataStore.edit { prefs ->
            val currentList = parseHistoryJson(prefs[KEY_HISTORY_JSON] ?: "[]").toMutableList()
            currentList.add(0, record)
            // 全量持久化保留，不限制 100 条上限，供 AI 统计长期回溯分析
            prefs[KEY_HISTORY_JSON] = serializeHistoryJson(currentList)
        }
    }

    /**
     * 安全清理前端显示：记录当前时间戳，仅在界面隐藏早于此时间的记录，数据库数据完全保留
     */
    suspend fun clearHistoryDisplay() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY_CLEAR_TIMESTAMP] = System.currentTimeMillis().toString()
        }
    }

    /**
     * 恢复显示全部历史记录
     */
    suspend fun resetHistoryDisplay() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY_CLEAR_TIMESTAMP] = "0"
        }
    }

    // 兼容旧接口
    suspend fun clearHistory() {
        clearHistoryDisplay()
    }

    // App 专属自律规则获取
    suspend fun getEffectiveRuleForApp(packageName: String, appName: String): AppRuleProfile {
        val map = appRulesMap.first()
        val custom = map[packageName]
        return if (custom != null && custom.isCustom) {
            custom
        } else {
            AppIntentContextHelper.getDefaultRuleProfile(packageName, appName)
        }
    }

    suspend fun saveAppRule(profile: AppRuleProfile) {
        context.dataStore.edit { prefs ->
            val map = parseAppRulesMap(prefs[KEY_APP_RULES_JSON] ?: "{}").toMutableMap()
            map[profile.packageName] = profile.copy(isCustom = true)
            prefs[KEY_APP_RULES_JSON] = serializeAppRulesMap(map)
        }
    }

    suspend fun resetAppRule(packageName: String) {
        context.dataStore.edit { prefs ->
            val map = parseAppRulesMap(prefs[KEY_APP_RULES_JSON] ?: "{}").toMutableMap()
            map.remove(packageName)
            prefs[KEY_APP_RULES_JSON] = serializeAppRulesMap(map)
        }
    }

    suspend fun isPackageBlocked(packageName: String): Boolean {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[KEY_PROTECTION_ENABLED] ?: true
        if (!enabled) return false
        val blacklist = prefs[KEY_BLACKLIST] ?: emptySet()
        return blacklist.contains(packageName)
    }

    suspend fun getEffectiveSystemPrompt(): String {
        val prefs = context.dataStore.data.first()
        val personaId = prefs[KEY_PERSONA] ?: PersonaProfile.BUILT_IN_NEUTRAL.id
        val customList = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]")
        val all = PersonaProfile.BUILT_IN_PERSONAS + customList
        val active = all.firstOrNull { it.id == personaId } ?: PersonaProfile.BUILT_IN_NEUTRAL
        return active.buildEffectivePrompt()
    }

    suspend fun getEffectiveSystemPromptForApp(packageName: String, appName: String): String {
        val basePrompt = getEffectiveSystemPrompt()
        val appRule = getEffectiveRuleForApp(packageName, appName)
        val rulePrompt = AppIntentContextHelper.buildAppRulePrompt(appRule)
        return "$basePrompt\n\n$rulePrompt"
    }

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val blacklisted = blacklistedPackages.first()
        val ownPackage = context.packageName

        resolveInfos.mapNotNull { resolveInfo ->
            val pkgName = resolveInfo.activityInfo.packageName
            if (pkgName == ownPackage) return@mapNotNull null

            val appName = resolveInfo.loadLabel(pm).toString()
            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }

            AppInfo(
                packageName = pkgName,
                appName = appName,
                isBlocked = blacklisted.contains(pkgName),
                isSystemApp = isSystem
            )
        }.distinctBy { it.packageName }
            .sortedWith(compareByDescending<AppInfo> { it.isBlocked }.thenBy { it.appName })
    }

    private fun parseStatsReportsCacheJson(jsonStr: String): Map<String, StatsReport> {
        return try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, StatsReport>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                map[key] = StatsReport.fromJson(obj)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeStatsReportsCacheJson(map: Map<String, StatsReport>): String {
        val json = JSONObject()
        map.forEach { (key, report) ->
            json.put(key, report.toJson())
        }
        return json.toString()
    }

    private fun parseCloudConfigsMap(jsonStr: String): Map<String, CloudProviderConfig> {
        return try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, CloudProviderConfig>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                map[key] = CloudProviderConfig.fromJson(obj)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeCloudConfigsMap(map: Map<String, CloudProviderConfig>): String {
        val json = JSONObject()
        map.forEach { (key, config) ->
            json.put(key, config.toJson())
        }
        return json.toString()
    }

    private fun parseAppRulesMap(jsonStr: String): Map<String, AppRuleProfile> {
        return try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, AppRuleProfile>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                map[key] = AppRuleProfile.fromJson(obj)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeAppRulesMap(map: Map<String, AppRuleProfile>): String {
        val json = JSONObject()
        map.forEach { (key, profile) ->
            json.put(key, profile.toJson())
        }
        return json.toString()
    }

    private fun parseCustomPersonasJson(jsonStr: String): List<PersonaProfile> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<PersonaProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(PersonaProfile.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeCustomPersonasJson(profiles: List<PersonaProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(profile.toJson())
        }
        return array.toString()
    }

    private fun parseHistoryJson(jsonStr: String): List<ApprovalRecord> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ApprovalRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ApprovalRecord(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        reason = obj.optString("reason"),
                        approved = obj.optBoolean("approved"),
                        comment = obj.optString("comment"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        allowedMinutes = obj.optInt("allowedMinutes", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeHistoryJson(records: List<ApprovalRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("packageName", record.packageName)
                put("appName", record.appName)
                put("reason", record.reason)
                put("approved", record.approved)
                put("comment", record.comment)
                put("timestamp", record.timestamp)
                put("allowedMinutes", record.allowedMinutes)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
