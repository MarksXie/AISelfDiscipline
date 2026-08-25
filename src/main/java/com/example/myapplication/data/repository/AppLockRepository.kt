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
import com.example.myapplication.data.model.CloudProviderPreset
import com.example.myapplication.data.model.PersonaProfile
import com.example.myapplication.data.model.PersonaType
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

        // 云端大模型相关配置键
        private val KEY_ENGINE_TYPE = stringPreferencesKey("ai_engine_type")
        private val KEY_CLOUD_PROVIDER = stringPreferencesKey("cloud_provider_preset")
        private val KEY_CLOUD_CONFIGS_MAP_JSON = stringPreferencesKey("cloud_configs_map_json")
    }

    val isProtectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROTECTION_ENABLED] ?: true
    }

    val blacklistedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLACKLIST] ?: emptySet()
    }

    // 自定义审查官人格列表
    val customPersonas: Flow<List<PersonaProfile>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]"
        parseCustomPersonasJson(jsonStr)
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

    // 引擎模式：默认 CLOUD（推荐）
    val engineType: Flow<AIEngineType> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ENGINE_TYPE] ?: AIEngineType.CLOUD.id
        AIEngineType.fromId(id)
    }

    // 各服务商配置字典
    val cloudConfigsMap: Flow<Map<String, CloudProviderConfig>> = context.dataStore.data.map { prefs ->
        parseCloudConfigsMap(prefs[KEY_CLOUD_CONFIGS_MAP_JSON] ?: "{}")
    }

    // 当前激活的服务商预设
    val cloudProvider: Flow<CloudProviderPreset> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_CLOUD_PROVIDER] ?: CloudProviderPreset.DEEPSEEK.id
        CloudProviderPreset.fromId(id)
    }

    // 当前激活服务商的完整独立配置
    val activeCloudConfig: Flow<CloudProviderConfig> = context.dataStore.data.map { prefs ->
        val providerId = prefs[KEY_CLOUD_PROVIDER] ?: CloudProviderPreset.DEEPSEEK.id
        val preset = CloudProviderPreset.fromId(providerId)
        val map = parseCloudConfigsMap(prefs[KEY_CLOUD_CONFIGS_MAP_JSON] ?: "{}")
        map[providerId] ?: CloudProviderConfig.getDefault(preset)
    }

    val cloudApiKey: Flow<String> = activeCloudConfig.map { it.apiKey }
    val cloudBaseUrl: Flow<String> = activeCloudConfig.map { it.baseUrl.ifBlank { CloudProviderPreset.fromId(it.providerId).defaultBaseUrl } }
    val cloudModelName: Flow<String> = activeCloudConfig.map { it.modelName.ifBlank { CloudProviderPreset.fromId(it.providerId).defaultModel } }

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

    suspend fun setPersona(persona: PersonaType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSONA] = persona.id
        }
    }

    suspend fun setActivePersonaId(personaId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSONA] = personaId
        }
    }

    suspend fun saveCustomPersona(profile: PersonaProfile) {
        context.dataStore.edit { prefs ->
            val currentList = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]").toMutableList()
            val index = currentList.indexOfFirst { it.id == profile.id }
            if (index != -1) {
                currentList[index] = profile
            } else {
                currentList.add(profile)
            }
            prefs[KEY_CUSTOM_PERSONAS_JSON] = serializeCustomPersonasJson(currentList)
            prefs[KEY_PERSONA] = profile.id
        }
    }

    suspend fun deleteCustomPersona(personaId: String) {
        context.dataStore.edit { prefs ->
            val currentList = parseCustomPersonasJson(prefs[KEY_CUSTOM_PERSONAS_JSON] ?: "[]").toMutableList()
            currentList.removeAll { it.id == personaId }
            prefs[KEY_CUSTOM_PERSONAS_JSON] = serializeCustomPersonasJson(currentList)

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

    suspend fun setActiveCloudProvider(provider: CloudProviderPreset) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLOUD_PROVIDER] = provider.id
        }
    }

    suspend fun saveCloudConfig(
        provider: CloudProviderPreset,
        apiKey: String,
        baseUrl: String,
        modelName: String,
        enableThinking: Boolean = false,
        thinkingParamKey: String = provider.defaultThinkingKey
    ) {
        context.dataStore.edit { prefs ->
            val map = parseCloudConfigsMap(prefs[KEY_CLOUD_CONFIGS_MAP_JSON] ?: "{}").toMutableMap()
            map[provider.id] = CloudProviderConfig(
                providerId = provider.id,
                apiKey = apiKey.trim(),
                baseUrl = baseUrl.trim().ifBlank { provider.defaultBaseUrl },
                modelName = modelName.trim().ifBlank { provider.defaultModel },
                enableThinking = enableThinking,
                thinkingParamKey = thinkingParamKey.trim().ifBlank { provider.defaultThinkingKey }
            )
            prefs[KEY_CLOUD_CONFIGS_MAP_JSON] = serializeCloudConfigsMap(map)
            prefs[KEY_CLOUD_PROVIDER] = provider.id
        }
    }

    // 兼容老调用
    suspend fun setCloudConfig(
        provider: CloudProviderPreset,
        apiKey: String,
        baseUrl: String,
        modelName: String
    ) {
        saveCloudConfig(provider, apiKey, baseUrl, modelName)
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

    suspend fun addHistoryRecord(record: ApprovalRecord) {
        context.dataStore.edit { prefs ->
            val currentList = parseHistoryJson(prefs[KEY_HISTORY_JSON] ?: "[]").toMutableList()
            currentList.add(0, record)
            if (currentList.size > 100) {
                currentList.removeAt(currentList.lastIndex)
            }
            prefs[KEY_HISTORY_JSON] = serializeHistoryJson(currentList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = "[]"
        }
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
