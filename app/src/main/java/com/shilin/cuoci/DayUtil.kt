package com.shilin.cuoci

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 一天的边界：早上 6 点到次日 5:59 算同一天。
 * 因为很多人习惯凌晨背单词，凌晨背的应该算前一天。
 */
object DayUtil {

    const val OFFSET_HOUR = 6

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 取「偏移 N 天」的那个逻辑日（0 = 今天） */
    private fun shifted(offsetDays: Int): Date {
        val c = Calendar.getInstance()
        if (c.get(Calendar.HOUR_OF_DAY) < OFFSET_HOUR) {
            c.add(Calendar.DAY_OF_YEAR, -1)
        }
        c.add(Calendar.DAY_OF_YEAR, -offsetDays)
        return c.time
    }

    fun todayKey(): String = fmt.format(shifted(0))

    fun keyOffset(daysAgo: Int): String = fmt.format(shifted(daysAgo))

    /** 2026-11-10 → 11/10 */
    fun shortLabel(key: String): String = key.substring(5).replace("-", "/")

    /** 前一天（用于凌晨数据迁移） */
    fun prevKey(day: String): String {
        return try {
            val c = Calendar.getInstance()
            c.time = fmt.parse(day) ?: return day
            c.add(Calendar.DAY_OF_YEAR, -1)
            fmt.format(c.time)
        } catch (e: Exception) {
            day
        }
    }

    /** 某个时间戳“算哪一天”（考虑凌晨偏移） */
    fun keyOf(timeMs: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = timeMs
        if (c.get(Calendar.HOUR_OF_DAY) < OFFSET_HOUR) {
            c.add(Calendar.DAY_OF_YEAR, -1)
        }
        return fmt.format(c.time)
    }

    /** 图表用的「日」标签 */
    fun dayLabel(daysAgo: Int): String {
        val c = Calendar.getInstance()
        c.time = shifted(daysAgo)
        return c.get(Calendar.DAY_OF_MONTH).toString()
    }
}
