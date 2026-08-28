package com.example.myapplication.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.myapplication.AppApplication
import com.example.myapplication.ai.AIEngine
import com.example.myapplication.ai.CloudOpenAIEngine
import com.example.myapplication.ai.LlamaCppEngine
import com.example.myapplication.data.model.AIEngineType
import com.example.myapplication.data.model.ApprovalRecord
import com.example.myapplication.data.model.PersonaType
import com.example.myapplication.data.repository.AppLockRepository
import com.example.myapplication.data.repository.WhitelistSessionManager
import com.example.myapplication.ui.overlay.InterceptOverlayContent
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object OverlayWindowManager {
    private const val TAG = "OverlayWindowManager"

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var overlayLifecycleOwner: MyOverlayLifecycleOwner? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val localEngine = LlamaCppEngine()
    private val cloudEngine = CloudOpenAIEngine()
    private var isModelReady by mutableStateOf(false)
    private var currentPackageName: String? = null

    fun show(context: Context, packageName: String, isTest: Boolean = false) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        mainHandler.post {
            if (composeView != null) {
                // If overlay is already active for the same app, do not recreate
                if (currentPackageName == packageName) return@post
                dismiss()
            }

            currentPackageName = packageName
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val repository = AppApplication.instance.repository
            val pm = context.packageManager
            val appName = try {
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                packageName
            }
            val appIcon: Drawable? = try {
                pm.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }

            // 初始化生命周期环境供 ComposeView 使用
            val lifecycleOwner = MyOverlayLifecycleOwner().apply {
                performRestore(null)
                handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                handleLifecycleEvent(Lifecycle.Event.ON_START)
                handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
            overlayLifecycleOwner = lifecycleOwner

            val newComposeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)

                setContent {
                    MyApplicationTheme {
                        val activePersona by repository.activePersona.collectAsState(initial = com.example.myapplication.data.model.PersonaProfile.BUILT_IN_NEUTRAL)

                        InterceptOverlayContent(
                            targetPackageName = packageName,
                            targetAppName = appName,
                            targetAppIcon = appIcon,
                            persona = PersonaType.NEUTRAL_EVALUATOR,
                            personaTitle = activePersona.title,
                            isModelReady = isModelReady,
                            isTest = isTest,
                            onEvaluateConversation = { history, callback ->
                                scope.launch {
                                    val systemPrompt = repository.getEffectiveSystemPromptForApp(packageName, appName)
                                    val engineType = repository.engineType.first()
                                    val activeEngine: AIEngine = if (engineType == AIEngineType.CLOUD) {
                                        val cloudCfg = repository.activeCloudConfig.first()
                                        cloudEngine.apiKey = cloudCfg.apiKey
                                        cloudEngine.baseUrl = cloudCfg.baseUrl
                                        cloudEngine.modelName = cloudCfg.modelName
                                        cloudEngine
                                    } else {
                                        localEngine
                                    }
                                    val result = activeEngine.evaluateConversation(history, appName, systemPrompt)
                                    callback(result)
                                }
                            },
                            onConfirmPass = { fullSummary, minutes, evaluationResult ->
                                scope.launch {
                                    // 仅真实应用拦截才记录日志与下发通行令牌，测试模式不写历史
                                    val isRealIntercept = !isTest && packageName != context.packageName
                                    if (isRealIntercept) {
                                        // 1. 赋予时效通行令牌
                                        WhitelistSessionManager.grantAccess(packageName, minutes)

                                        // 2. 记录真实审批日志（存储完整多轮攻防纪要）
                                        repository.addHistoryRecord(
                                            ApprovalRecord(
                                                packageName = packageName,
                                                appName = appName,
                                                reason = fullSummary,
                                                approved = true,
                                                comment = evaluationResult.comment,
                                                allowedMinutes = minutes
                                            )
                                        )
                                    }

                                    // 3. 关闭拦截悬浮窗并释放 AI 引擎
                                    dismiss()

                                    // 4. 真实拦截模式下自动拉起目标应用
                                    if (isRealIntercept) {
                                        withContext(Dispatchers.Main) {
                                            val launchIntent = pm.getLaunchIntentForPackage(packageName)?.apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                            }
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            }
                                        }
                                    }
                                }
                            },
                            onDismiss = { fullSummary, evaluationResult ->
                                scope.launch {
                                    val isRealIntercept = !isTest && packageName != context.packageName
                                    if (isRealIntercept && fullSummary.isNotBlank()) {
                                        // 无论是在追问下主动放弃还是终审驳回，均归档为自律拦截成功记录
                                        val commentText = if (evaluationResult != null && !evaluationResult.approved) {
                                            evaluationResult.comment
                                        } else {
                                            "在 AI 追问下主动放弃，成功抵制无明确目的的使用"
                                        }
                                        repository.addHistoryRecord(
                                            ApprovalRecord(
                                                packageName = packageName,
                                                appName = appName,
                                                reason = fullSummary,
                                                approved = false,
                                                comment = commentText,
                                                allowedMinutes = 0
                                            )
                                        )
                                    }
                                    dismiss()
                                }
                            }
                        )
                    }
                }
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager?.addView(newComposeView, layoutParams)
                composeView = newComposeView
                Log.i(TAG, "Overlay window added successfully for $packageName")

                // 异步触发模型即用即载预热
                scope.launch {
                    val engineType = repository.engineType.first()
                    if (engineType == AIEngineType.CLOUD) {
                        val cloudCfg = repository.activeCloudConfig.first()
                        cloudEngine.apiKey = cloudCfg.apiKey
                        cloudEngine.baseUrl = cloudCfg.baseUrl
                        cloudEngine.modelName = cloudCfg.modelName
                        isModelReady = cloudCfg.apiKey.isNotBlank()
                    } else {
                        val modelPath = repository.modelPath.first()
                        isModelReady = localEngine.preheat(modelPath)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add overlay view", e)
            }
        }
    }

    fun dismiss() {
        mainHandler.post {
            composeView?.let { view ->
                overlayLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                overlayLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                overlayLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing overlay view", e)
                }
            }
            composeView = null
            overlayLifecycleOwner = null
            currentPackageName = null
            isModelReady = false

            // 释放模型占用的资源
            localEngine.release()
            cloudEngine.release()
            Log.i(TAG, "Overlay dismissed and AI engine resources released.")
        }
    }

    fun isShowing(): Boolean = composeView != null
}

private class MyOverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
