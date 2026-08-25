package com.example.myapplication.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader

object DeviceInfoHelper {

    /**
     * 获取设备品牌与型号，例如 "Xiaomi 23116PN5BC"、"Google Pixel 8 Pro"
     */
    fun getDeviceBrandAndModel(): String {
        val brand = Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL
        return if (model.startsWith(brand, ignoreCase = true)) {
            model
        } else {
            "$brand $model"
        }
    }

    /**
     * 获取处理器 SoC 名称或硬件代号，例如 "Snapdragon 8 Gen 3"、"Tensor G3"、"SM8650"
     */
    fun getSocProcessorName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val socModel = Build.SOC_MODEL
                if (!socModel.isNullOrBlank() && socModel != "unknown") {
                    return socModel
                }
            }

            val hardware = Build.HARDWARE
            if (!hardware.isNullOrBlank() && hardware != "unknown" && !hardware.startsWith("qcom", ignoreCase = true)) {
                return hardware
            }

            // 从 /proc/cpuinfo 读取 Hardware 信息
            val cpuInfo = readCpuInfoHardware()
            if (!cpuInfo.isNullOrBlank()) {
                return cpuInfo
            }

            // 备选：根据核心数展示
            val cores = Runtime.getRuntime().availableProcessors()
            "8核高性能处理器 (${cores}T)"
        } catch (e: Exception) {
            "移动终端芯片"
        }
    }

    /**
     * 获取总运行内存格式化字符串，例如 "12.0 GB"
     */
    fun getTotalMemoryFormatted(context: Context): String {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalBytes = memInfo.totalMem
            val gb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            String.format("%.1f GB", gb)
        } catch (e: Exception) {
            "8.0 GB"
        }
    }

    /**
     * 生成状态栏设备专属护航标语
     */
    fun getDeviceStatusBanner(context: Context): String {
        val device = getDeviceBrandAndModel()
        val soc = getSocProcessorName()
        return "$device · $soc 端侧护航"
    }

    private fun readCpuInfoHardware(): String? {
        return try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("Hardware", ignoreCase = true) == true) {
                        return@use line?.substringAfter(":")?.trim()
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
