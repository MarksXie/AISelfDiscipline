package com.example.myapplication.util

import com.example.myapplication.data.model.StatsPeriodType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StatsPeriodHelper {

    /**
     * 根据周期类型和偏移量（0表示当前，-1表示上一期，-2表示上上期等），返回：
     * Triple(startMs, endMs, displayLabel)
     */
    fun getPeriodRange(type: StatsPeriodType, offset: Int = 0): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        when (type) {
            StatsPeriodType.DAY -> {
                cal.add(Calendar.DAY_OF_YEAR, offset)
                val start = getDayStart(cal)
                val end = getDayEnd(cal)
                val label = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(start))
                return Triple(start, end, label)
            }

            StatsPeriodType.WEEK -> {
                cal.add(Calendar.WEEK_OF_YEAR, offset)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = getDayStart(cal)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val end = getDayEnd(cal)
                val df = SimpleDateFormat("MM.dd", Locale.getDefault())
                val yearDf = SimpleDateFormat("yyyy", Locale.getDefault())
                val label = "${yearDf.format(Date(start))}年 ${df.format(Date(start))}~${df.format(Date(end))}"
                return Triple(start, end, label)
            }

            StatsPeriodType.MONTH -> {
                cal.add(Calendar.MONTH, offset)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = getDayStart(cal)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = getDayEnd(cal)
                val label = SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date(start))
                return Triple(start, end, label)
            }

            StatsPeriodType.QUARTER -> {
                val currentMonth = cal.get(Calendar.MONTH) // 0-11
                val currentQuarter = currentMonth / 3 // 0, 1, 2, 3
                cal.add(Calendar.MONTH, offset * 3)
                val quarterMonth = (cal.get(Calendar.MONTH) / 3) * 3
                cal.set(Calendar.MONTH, quarterMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = getDayStart(cal)
                cal.add(Calendar.MONTH, 2)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = getDayEnd(cal)
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(start))
                val qNumber = (quarterMonth / 3) + 1
                val label = "${year}年 第${qNumber}季度 (Q${qNumber})"
                return Triple(start, end, label)
            }

            StatsPeriodType.HALF_YEAR -> {
                val currentMonth = cal.get(Calendar.MONTH)
                val currentHalf = if (currentMonth < 6) 0 else 1
                cal.add(Calendar.MONTH, offset * 6)
                val isFirstHalf = cal.get(Calendar.MONTH) < 6
                cal.set(Calendar.MONTH, if (isFirstHalf) 0 else 6)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = getDayStart(cal)
                cal.set(Calendar.MONTH, if (isFirstHalf) 5 else 11)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = getDayEnd(cal)
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(start))
                val halfLabel = if (isFirstHalf) "上半年 (H1)" else "下半年 (H2)"
                val label = "${year}年 $halfLabel"
                return Triple(start, end, label)
            }

            StatsPeriodType.YEAR -> {
                cal.add(Calendar.YEAR, offset)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = getDayStart(cal)
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                val end = getDayEnd(cal)
                val label = SimpleDateFormat("yyyy年度", Locale.getDefault()).format(Date(start))
                return Triple(start, end, label)
            }
        }
    }

    /**
     * 生成报告的唯一持久化 Key，如 "day_2026-08-28", "week_2026-08-24", "month_2026-08", "year_2026"
     */
    fun getPeriodKey(type: StatsPeriodType, startMs: Long): String {
        val date = Date(startMs)
        return when (type) {
            StatsPeriodType.DAY -> "day_" + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            StatsPeriodType.WEEK -> "week_" + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            StatsPeriodType.MONTH -> "month_" + SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)
            StatsPeriodType.QUARTER -> {
                val cal = Calendar.getInstance().apply { time = date }
                val q = (cal.get(Calendar.MONTH) / 3) + 1
                "quarter_" + SimpleDateFormat("yyyy", Locale.getDefault()).format(date) + "_Q$q"
            }
            StatsPeriodType.HALF_YEAR -> {
                val cal = Calendar.getInstance().apply { time = date }
                val h = if (cal.get(Calendar.MONTH) < 6) "H1" else "H2"
                "half_year_" + SimpleDateFormat("yyyy", Locale.getDefault()).format(date) + "_$h"
            }
            StatsPeriodType.YEAR -> "year_" + SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
        }
    }

    /**
     * 分层级联：获取当前宏观周期内包含的子周期 Key 列表
     * - 周报 -> 7 份日报 Keys
     * - 月报 -> 该月内所有日报 Keys (或周报 Keys)
     * - 季报 -> 该季度内 3 个月报 Keys
     * - 半年报 -> 2 个季报 Keys (或 6 个月报 Keys)
     * - 年报 -> 4 个季报 Keys (或 2 个半年报 Keys)
     */
    fun getSubPeriodKeys(type: StatsPeriodType, startMs: Long, endMs: Long): List<String> {
        val keys = mutableListOf<String>()
        val cal = Calendar.getInstance()

        when (type) {
            StatsPeriodType.DAY -> {
                // 日报为最底层，无子报告
                return emptyList()
            }
            StatsPeriodType.WEEK, StatsPeriodType.MONTH -> {
                // 级联子项为日报
                cal.timeInMillis = startMs
                while (cal.timeInMillis < endMs) {
                    keys.add(getPeriodKey(StatsPeriodType.DAY, cal.timeInMillis))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            StatsPeriodType.QUARTER -> {
                // 级联子项为月报
                cal.timeInMillis = startMs
                while (cal.timeInMillis < endMs) {
                    keys.add(getPeriodKey(StatsPeriodType.MONTH, cal.timeInMillis))
                    cal.add(Calendar.MONTH, 1)
                }
            }
            StatsPeriodType.HALF_YEAR -> {
                // 级联子项为季报
                cal.timeInMillis = startMs
                while (cal.timeInMillis < endMs) {
                    keys.add(getPeriodKey(StatsPeriodType.QUARTER, cal.timeInMillis))
                    cal.add(Calendar.MONTH, 3)
                }
            }
            StatsPeriodType.YEAR -> {
                // 级联子项为 4 个季报
                cal.timeInMillis = startMs
                while (cal.timeInMillis < endMs) {
                    keys.add(getPeriodKey(StatsPeriodType.QUARTER, cal.timeInMillis))
                    cal.add(Calendar.MONTH, 3)
                }
            }
        }
        return keys
    }

    private fun getDayStart(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getDayEnd(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}
