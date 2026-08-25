package com.example.myapplication.data.model

import java.util.UUID

data class ApprovalRecord(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val reason: String,
    val approved: Boolean,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val allowedMinutes: Int = 0
)
