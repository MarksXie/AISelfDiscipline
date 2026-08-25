package com.example.myapplication.ai

import android.util.Log

object NativeLlamaBridge {
    private const val TAG = "NativeLlamaBridge"
    var isLibraryLoaded = false
        private set

    init {
        try {
            System.loadLibrary("llama_jni")
            isLibraryLoaded = true
            Log.i(TAG, "libllama_jni.so loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "libllama_jni.so not found, running in fallback mode. Reason: ${e.message}")
            isLibraryLoaded = false
        }
    }

    external fun nativeLoadModel(modelPath: String, nThreads: Int, nGpuLayers: Int, useNpu: Boolean): Long
    external fun nativeEvaluate(handle: Long, prompt: String, grammar: String): String
    external fun nativeFree(handle: Long)
}
