package com.shilin.cuoci

import android.content.Context
import org.json.JSONArray

/**
 * 内置离线词库（四级 + 六级 + 考研红宝书，共 7000+ 词）。
 * 查词先走本地，查不到再让 AI 补。
 */
object WordBank {

    class Entry(val phonetic: String, val meaning: String)

    @Volatile
    private var map: HashMap<String, Entry>? = null

    private var loading = false

    fun init(ctx: Context) {
        if (map != null || loading) return
        loading = true
        val app = ctx.applicationContext
        Thread {
            try {
                val text = app.assets.open("wordbank.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val arr = JSONArray(text)
                val m = HashMap<String, Entry>(arr.length() * 2)
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONArray(i)
                    val w = a.optString(0, "").lowercase()
                    if (w.isEmpty()) continue
                    m[w] = Entry(a.optString(1, ""), a.optString(2, ""))
                }
                map = m
                LogStore.add("BANK", "词库已加载：" + m.size + " 词")
            } catch (e: Exception) {
                LogStore.add("BANK", "词库加载失败：" + e.message)
            } finally {
                loading = false
            }
        }.start()
    }

    fun isReady(): Boolean = map != null

    fun size(): Int = map?.size ?: 0

    /** 查词（自动处理常见变形） */
    fun lookup(word: String): Entry? {
        val m = map ?: return null
        val k = normalize(word)
        if (k.isEmpty()) return null

        m[k]?.let { return it }

        // 常见后缀
        val suffixes = arrayOf("s", "es", "ed", "d", "ing", "ly", "er", "est", "ies")
        for (s in suffixes) {
            if (k.endsWith(s) && k.length > s.length + 2) {
                val base = k.substring(0, k.length - s.length)
                m[base]?.let { return it }
                if (s == "ies") {
                    m[base + "y"]?.let { return it }
                }
                // 双写辅音：stopped -> stop
                if (base.length > 3) {
                    val c = base[base.length - 1]
                    if (base[base.length - 2] == c && "bdgmnprt".contains(c)) {
                        m[base.substring(0, base.length - 1)]?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun normalize(s: String): String {
        return s.lowercase().replace("·", "").replace("-", "").replace(" ", "").trim()
    }
}
