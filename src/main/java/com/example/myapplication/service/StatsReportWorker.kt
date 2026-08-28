package com.example.myapplication.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.ai.StatsAIAnalyzer
import com.example.myapplication.data.model.StatsPeriodType

class StatsReportWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "StatsReportWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val periodTypeId = inputData.getString("periodType") ?: StatsPeriodType.DAY.id
            val periodType = StatsPeriodType.fromId(periodTypeId)

            Log.i(TAG, "Starting periodic stats report generation for: ${periodType.label}")
            val report = StatsAIAnalyzer.generateReport(context, periodType, offset = 0)

            if (!report.isError) {
                Log.i(TAG, "Successfully generated and cached stats report: ${report.periodKey}")
                Result.success()
            } else {
                Log.w(TAG, "Report generation completed with error: ${report.errorMessage}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run StatsReportWorker", e)
            Result.retry()
        }
    }
}
