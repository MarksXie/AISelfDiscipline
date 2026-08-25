package com.example.myapplication.data.repository

import java.util.concurrent.ConcurrentHashMap

object WhitelistSessionManager {
    private val sessions = ConcurrentHashMap<String, Long>()

    fun grantAccess(packageName: String, minutes: Int) {
        val expireTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        sessions[packageName] = expireTime
    }

    fun isPackageAllowed(packageName: String): Boolean {
        val expireTime = sessions[packageName] ?: return false
        if (System.currentTimeMillis() <= expireTime) {
            return true
        }
        // Expired
        sessions.remove(packageName)
        return false
    }

    fun getRemainingMillis(packageName: String): Long {
        val expireTime = sessions[packageName] ?: return 0L
        val remaining = expireTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun revokeAccess(packageName: String) {
        sessions.remove(packageName)
    }

    fun clearAll() {
        sessions.clear()
    }
}
