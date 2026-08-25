package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.AppApplication
import com.example.myapplication.data.repository.WhitelistSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var launcherPackages = setOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AppLockAccessibilityService connected.")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 50
        serviceInfo = info
        refreshLauncherPackages()

        // 启动前台保活服务
        KeepAliveForegroundService.startService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // 忽略自身、桌面启动器、系统UI及输入法
        if (packageName == applicationContext.packageName ||
            packageName == "com.android.systemui" ||
            packageName == "android" ||
            launcherPackages.contains(packageName)
        ) {
            return
        }

        serviceScope.launch {
            val repository = AppApplication.instance.repository

            // 1. 检查是否在黑名单中
            val isBlocked = repository.isPackageBlocked(packageName)
            if (!isBlocked) return@launch

            // 2. 检查是否处于已放行的会话时间窗口内
            if (WhitelistSessionManager.isPackageAllowed(packageName)) {
                Log.d(TAG, "Package $packageName is in active whitelist session, skipping intercept.")
                return@launch
            }

            Log.i(TAG, "Intercepted blacklisted app: $packageName, className: $className")

            // 3. 立即将目标 App 压入后台，防止用户在 AI 判断期间看到/操作目标 App
            performGlobalAction(GLOBAL_ACTION_HOME)

            // 4. 弹出全屏 AI 拦截悬浮窗
            OverlayWindowManager.show(applicationContext, packageName)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppLockAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        OverlayWindowManager.dismiss()
        Log.i(TAG, "AppLockAccessibilityService destroyed.")
    }

    private fun refreshLauncherPackages() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            launcherPackages = resolveInfos.map { it.activityInfo.packageName }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying launcher packages", e)
        }
    }

    companion object {
        private const val TAG = "AccessibilityLock"
    }
}
