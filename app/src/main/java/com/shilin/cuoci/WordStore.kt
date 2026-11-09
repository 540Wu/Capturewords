package com.shilin.cuoci

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 错词存储：按天分组，存成 JSON 文件（应用私有目录），不依赖数据库。
 */
object WordStore {

    data class Rec(
        val word: String,
        val reason: String,
        val time: Long,
        val phonetic: String = "",
        val meaning: String = ""
    )

    private var ctx: Context? = null
    private var loaded = false
    private val data = LinkedHashMap<String, MutableList<Rec>>()
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    @Synchronized
    fun init(c: Context) {
        ctx = c.applicationContext
        if (!loaded) {
            load()
            loaded = true
        }
    }

    @Synchronized
    fun add(word: String, reason: String, phonetic: String = "", meaning: String = "") {
        val w = word.trim()
        if (w.isEmpty()) return
        val day = todayKey()
        val list = data.getOrPut(day) { ArrayList() }
        // 同一天同一个词只记一次；若之前没存到音标/释义，这里补上
        for (i in list.indices) {
            val r = list[i]
            if (r.word.equals(w, ignoreCase = true)) {
                if ((r.phonetic.isEmpty() && phonetic.isNotEmpty()) ||
                    (r.meaning.isEmpty() && meaning.isNotEmpty())
                ) {
                    list[i] = Rec(
                        r.word, r.reason, r.time,
                        if (r.phonetic.isEmpty()) phonetic else r.phonetic,
                        if (r.meaning.isEmpty()) meaning else r.meaning
                    )
                    save()
                }
                return
            }
        }
        list.add(Rec(w, reason, System.currentTimeMillis(), phonetic, meaning))
        save()
        LogStore.add("REC", "记录错词 " + w + "（" + reason + "）")
    }

    @Synchronized
    fun today(): List<Rec> = data[todayKey()]?.toList() ?: emptyList()

    @Synchronized
    fun days(): List<String> = data.keys.sortedDescending()

    @Synchronized
    fun of(day: String): List<Rec> = data[day]?.toList() ?: emptyList()

    @Synchronized
    fun clearToday() {
        data.remove(todayKey())
        save()
    }

    /** 删除某天的某个词。 */
    @Synchronized
    fun remove(day: String, word: String) {
        val list = data[day] ?: return
        val ok = list.removeAll { it.word.equals(word, ignoreCase = true) }
        if (ok) {
            if (list.isEmpty()) data.remove(day)
            save()
        }
    }

    /** 补全某天某个词的音标和释义。 */
    @Synchronized
    fun updateMeaning(day: String, word: String, phonetic: String, meaning: String) {
        val list = data[day] ?: return
        for (i in list.indices) {
            val r = list[i]
            if (r.word.equals(word, ignoreCase = true)) {
                list[i] = Rec(
                    r.word, r.reason, r.time,
                    if (phonetic.isEmpty()) r.phonetic else phonetic,
                    if (meaning.isEmpty()) r.meaning else meaning
                )
            }
        }
        save()
    }

    /** 所有还没释义的词（日期 + 记录）。 */
    @Synchronized
    fun missingMeaning(): List<Pair<String, Rec>> {
        val out = ArrayList<Pair<String, Rec>>()
        for ((day, list) in data) {
            for (r in list) {
                if (r.meaning.isEmpty()) out.add(Pair(day, r))
            }
        }
        return out
    }

    fun todayKey(): String = dayFmt.format(Date(System.currentTimeMillis()))

    fun timeText(t: Long): String = timeFmt.format(Date(t))

    private fun file(): File? = ctx?.let { File(it.filesDir, "records.json") }

    private fun load() {
        val f = file() ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            for (day in root.keys()) {
                val arr = root.getJSONArray(day)
                val list = ArrayList<Rec>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Rec(
                            o.getString("w"),
                            o.optString("r"),
                            o.optLong("t"),
                            o.optString("ph"),
                            o.optString("m")
                        )
                    )
                }
                data[day] = list
            }
        } catch (e: Exception) {
            LogStore.add("STORE", "读取失败：" + e.message)
        }
    }

    private fun save() {
        val f = file() ?: return
        try {
            val root = JSONObject()
            for ((day, list) in data) {
                val arr = JSONArray()
                for (r in list) {
                    val o = JSONObject()
                    o.put("w", r.word)
                    o.put("r", r.reason)
                    o.put("t", r.time)
                    o.put("ph", r.phonetic)
                    o.put("m", r.meaning)
                    arr.put(o)
                }
                root.put(day, arr)
            }
            f.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            LogStore.add("STORE", "保存失败：" + e.message)
        }
    }
}
