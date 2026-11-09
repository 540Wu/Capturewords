package com.shilin.cuoci

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用 DeepSeek 批量补全单词的音标和中文释义。
 */
object MeaningFiller {

    private const val API_URL = "https://api.deepseek.com/chat/completions"

    class Item(val word: String, val phonetic: String, val meaning: String)

    fun fill(apiKey: String, words: List<String>): List<Item> {
        if (words.isEmpty()) return emptyList()
        val resp = post(apiKey, buildPrompt(words))
        return parse(resp)
    }

    private fun buildPrompt(words: List<String>): String {
        val sb = StringBuilder()
        sb.append("请给出下面每个英语单词的音标和中文释义。要求：\n")
        sb.append("1. 音标用美式或英式均可，用 / / 包起来；\n")
        sb.append("2. 中文释义写成「词性. 中文」的形式，最多 3 个义项，简洁明了；\n")
        sb.append("3. 只输出一个 JSON 数组，不要输出其他任何内容，格式为：\n")
        sb.append("[{\"w\":\"单词\",\"ph\":\"/音标/\",\"m\":\"词性. 释义\"}]\n\n")
        sb.append("单词列表：")
        sb.append(words.joinToString(", "))
        return sb.toString()
    }

    private fun post(apiKey: String, userPrompt: String): String {
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
        body.put("temperature", 0.3)
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
        if (code !in 200..299) throw RuntimeException("HTTP " + code + "：" + text.take(200))

        val root = JSONObject(text)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) throw RuntimeException("返回里没有内容")
        return choices.getJSONObject(0).getJSONObject("message").optString("content", "")
    }

    private fun parse(content: String): List<Item> {
        val start = content.indexOf('[')
        val end = content.lastIndexOf(']')
        if (start < 0 || end <= start) throw RuntimeException("模型返回格式不对")

        val arr = JSONArray(content.substring(start, end + 1))
        val out = ArrayList<Item>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val w = o.optString("w", "").trim()
            if (w.isEmpty()) continue
            val ph = o.optString("ph", "").replace("*", "").trim()
            val m = o.optString("m", "").replace("*", "").replace("\n", " ").trim()
            out.add(Item(w, ph, m))
        }
        return out
    }
}
