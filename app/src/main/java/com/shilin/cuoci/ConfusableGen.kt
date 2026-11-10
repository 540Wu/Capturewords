package com.shilin.cuoci

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 易混淆词：优先用 AI 生成（拼写/发音/意思易混的词），没 Key 就用本地相似度算法。
 */
object ConfusableGen {

    private const val API_URL = "https://api.deepseek.com/chat/completions"

    class Item(val word: String, val confusables: List<String>, val tip: String)

    /** AI 生成 */
    fun ai(apiKey: String, words: List<String>): List<Item> {
        val prompt = StringBuilder()
        prompt.append("下面是学生今天记错的英语单词。请为每个词找出 1-2 个「易混淆词」")
        prompt.append("（拼写相近、发音相近，或者意思容易搞混的词），并用一句中文说清区别。\n")
        prompt.append("只输出一个 JSON 数组，不要任何解释，格式：\n")
        prompt.append("[{\"w\":\"原词\",\"c\":[\"易混词1\",\"易混词2\"],\"tip\":\"一句话说明区别（30字内）\"}]\n\n")
        prompt.append("单词：")
        prompt.append(words.joinToString(", "))

        val content = request(apiKey, prompt.toString(), minOf(200 + words.size * 55, 1200))
        val s = content.indexOf('[')
        val e = content.lastIndexOf(']')
        if (s < 0 || e <= s) throw RuntimeException("返回格式不对")
        val arr = JSONArray(content.substring(s, e + 1))

        val out = ArrayList<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val w = o.optString("w", "").replace("*", "").trim()
            if (w.isEmpty()) continue
            val cArr = o.optJSONArray("c")
            val list = ArrayList<String>()
            if (cArr != null) {
                for (j in 0 until cArr.length()) list.add(cArr.getString(j).replace("*", ""))
            }
            out.add(Item(w, list, o.optString("tip", "").replace("*", "")))
        }
        if (out.isEmpty()) throw RuntimeException("没解析出内容")
        return out
    }

    /** 本地兜底：在错词之间两两比对拼写相似度 */
    fun local(words: List<String>): List<Item> {
        val out = ArrayList<Item>()
        val used = HashSet<String>()

        for (i in words.indices) {
            val a = words[i]
            val similar = ArrayList<String>()
            for (j in words.indices) {
                if (i == j) continue
                val b = words[j]
                if (similarity(a, b) >= 0.6) similar.add(b)
            }
            if (similar.isNotEmpty()) {
                out.add(Item(a, similar, "拼写相似，注意区分"))
                used.add(a)
            }
        }

        if (out.isEmpty()) {
            out.add(Item("—", emptyList(), "今天的错词之间没有拼写特别接近的。填个 API Key 可以让 AI 帮你找形近词和近义词。"))
        }
        return out
    }

    /** 相似度（1 - 编辑距离/较长长度） */
    fun similarity(a: String, b: String): Double {
        val x = a.lowercase()
        val y = b.lowercase()
        if (x.isEmpty() || y.isEmpty()) return 0.0
        val d = levenshtein(x, y)
        return 1.0 - d.toDouble() / maxOf(x.length, y.length)
    }

    // 缓存序列化
    fun toJson(items: List<Item>): String {
        val arr = JSONArray()
        for (it in items) {
            val o = JSONObject()
            o.put("w", it.word)
            o.put("c", JSONArray(it.confusables))
            o.put("tip", it.tip)
            arr.put(o)
        }
        return arr.toString()
    }

    fun fromJson(json: String): List<Item> {
        val out = ArrayList<Item>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cArr = o.optJSONArray("c")
                val list = ArrayList<String>()
                if (cArr != null) for (j in 0 until cArr.length()) list.add(cArr.getString(j))
                out.add(Item(o.optString("w"), list, o.optString("tip")))
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun levenshtein(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        var prev = IntArray(m + 1) { it }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            cur[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev
            prev = cur
            cur = t
        }
        return prev[m]
    }

    private fun request(apiKey: String, userPrompt: String, maxTokens: Int): String {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 90000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer " + apiKey)

        val body = JSONObject()
        body.put("model", "deepseek-chat")
        body.put("stream", false)
        body.put("temperature", 0.8)
        body.put("max_tokens", maxTokens)
        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "user").put("content", userPrompt))
        body.put("messages", msgs)

        val writer = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("HTTP " + code + "：" + text.take(160))

        val root = JSONObject(text)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) throw RuntimeException("没有返回内容")
        return choices.getJSONObject(0).getJSONObject("message").optString("content", "")
    }
}
