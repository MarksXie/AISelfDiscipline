package com.example.myapplication.ai

import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.EvaluationResult

interface AIEngine {
    val engineName: String
    suspend fun preheat(modelPath: String): Boolean
    
    // 多轮对话推理接口
    suspend fun evaluateConversation(
        conversationHistory: List<ChatMessage>,
        targetAppName: String,
        systemPrompt: String
    ): EvaluationResult

    // 单轮快捷推理接口（向后兼容）
    suspend fun evaluateReason(
        reason: String,
        targetAppName: String,
        systemPrompt: String
    ): EvaluationResult

    fun release()
    fun isReady(): Boolean
}
