package com.example.myapplication.data.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val isSystemApp: Boolean = false
)
