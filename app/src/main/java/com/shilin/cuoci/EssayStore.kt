package com.shilin.cuoci

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 按天缓存生成好的短文，避免每次点生成都重新调用 API。
 */
object EssayStore {

    class Entry(val words: List<String>, val parts: List<EssayGenerator.Part>)

    private var ctx: Context? = null
    private var loaded = false
    private val data = LinkedHashMap<String, Entry>()

    @Synchronized
    fun init(c: Context) {
        ctx = c.applicationContext
        if (!loaded) {
            load()
            loaded = true
        }
    }

    @Synchronized
    fun get(day: String): Entry? = data[day]

    @Synchronized
    fun save(day: String, words: List<String>, parts: List<EssayGenerator.Part>) {
        data[day] = Entry(words, parts)
        persist()
    }

    @Synchronized
    fun remove(day: String) {
        data.remove(day)
        persist()
    }

    private fun file(): File? = ctx?.let { File(it.filesDir, "essays.json") }

    private fun load() {
        val f = file() ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            for (day in root.keys()) {
                val o = root.getJSONObject(day)
                val words = jsonList(o.getJSONArray("words"))
                val parts = ArrayList<EssayGenerator.Part>()
                val arr = o.getJSONArray("parts")
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    parts.add(
                        EssayGenerator.Part(
                            jsonList(p.getJSONArray("w")),
                            p.optString("en", ""),
                            p.optString("zh", ""),
                            jsonList(p.getJSONArray("m"))
                        )
                    )
                }
                data[day] = Entry(words, parts)
            }
        } catch (e: Exception) {
            LogStore.add("STORE", "短文缓存读取失败：" + e.message)
        }
    }

    private fun persist() {
        val f = file() ?: return
        try {
            val root = JSONObject()
            for ((day, e) in data) {
                val o = JSONObject()
                o.put("words", JSONArray(e.words))
                val arr = JSONArray()
                for (p in e.parts) {
                    val po = JSONObject()
                    po.put("w", JSONArray(p.words))
                    po.put("en", p.english)
                    po.put("zh", p.chinese)
                    po.put("m", JSONArray(p.missing))
                    arr.put(po)
                }
                o.put("parts", arr)
                root.put(day, o)
            }
            f.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            LogStore.add("STORE", "短文缓存保存失败：" + e.message)
        }
    }

    private fun jsonList(a: JSONArray): List<String> {
        val out = ArrayList<String>(a.length())
        for (i in 0 until a.length()) out.add(a.getString(i))
        return out
    }
}
