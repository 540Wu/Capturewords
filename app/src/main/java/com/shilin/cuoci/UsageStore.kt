package com.shilin.cuoci

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * 记录每天在「不背单词」里的学习时长（毫秒）。
 */
object UsageStore {

    private var ctx: Context? = null
    private var loaded = false
    private val data = LinkedHashMap<String, Long>()

    @Synchronized
    fun init(c: Context) {
        ctx = c.applicationContext
        if (!loaded) {
            load()
            loaded = true
        }
    }

    @Synchronized
    fun add(ms: Long) {
        if (ms <= 0) return
        val day = WordStore.todayKey()
        data[day] = (data[day] ?: 0L) + ms
        persist()
    }

    @Synchronized
    fun todayMs(): Long = data[WordStore.todayKey()] ?: 0L

    @Synchronized
    fun dayMs(day: String): Long = data[day] ?: 0L

    /** 某个月（如 2026-11）的总时长。 */
    @Synchronized
    fun monthMs(prefix: String): Long {
        var sum = 0L
        for ((day, ms) in data) {
            if (day.startsWith(prefix)) sum += ms
        }
        return sum
    }

    @Synchronized
    fun totalMs(): Long {
        var sum = 0L
        for (v in data.values) sum += v
        return sum
    }

    /** 把毫秒格式化成「1 小时 23 分」这种。 */
    fun format(ms: Long): String {
        if (ms <= 0) return "0 分钟"
        val totalMin = ms / 60000
        if (totalMin < 1) return "不到 1 分钟"
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) h.toString() + " 小时 " + m + " 分" else m.toString() + " 分钟"
    }

    private fun file(): File? = ctx?.let { File(it.filesDir, "usage.json") }

    // ===== 备份 / 合并 =====

    @Synchronized
    fun exportData(): JSONObject {
        val root = JSONObject()
        for ((day, ms) in data) root.put(day, ms)
        return root
    }

    /** 同一天取较大值，避免重复累加。 */
    @Synchronized
    fun mergeData(other: JSONObject) {
        for (day in other.keys()) {
            val ms = other.optLong(day)
            val cur = data[day] ?: 0L
            if (ms > cur) data[day] = ms
        }
        persist()
    }

    private fun load() {
        val f = file() ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            for (day in root.keys()) {
                data[day] = root.optLong(day)
            }
        } catch (e: Exception) {
            LogStore.add("STORE", "时长读取失败：" + e.message)
        }
    }

    private fun persist() {
        val f = file() ?: return
        try {
            val root = JSONObject()
            for ((day, ms) in data) root.put(day, ms)
            f.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            LogStore.add("STORE", "时长保存失败：" + e.message)
        }
    }
}
