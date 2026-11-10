package com.shilin.cuoci

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 查近义词：结果按词缓存，同一个词永远只查一次。
 */
object SynonymGen {

    private const val API_URL = "https://api.deepseek.com/chat/completions"

    /** 先查缓存，没有才请求 */
    fun get(ctx: android.content.Context, apiKey: String, word: String): List<String> {
        val cached = Prefs.synonym(ctx, word)
        if (cached.isNotEmpty()) {
            return cached.split("|").filter { it.isNotBlank() }
        }
        if (apiKey.isEmpty()) return emptyList()

        val list = ask(apiKey, word)
        if (list.isNotEmpty()) Prefs.setSynonym(ctx, word, list.joinToString("|"))
        return list
    }

    private fun ask(apiKey: String, word: String): List<String> {
        val prompt = "请给出英语单词 \"$word\" 的 3-6 个意思最接近的同义词（只要单词原形，不要短语和解释）。" +
            "只输出用逗号分隔的单词，不要任何其他内容。"

        val conn = URL(API_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 40000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer " + apiKey)

        val body = JSONObject()
        body.put("model", "deepseek-chat")
        body.put("stream", false)
        body.put("temperature", 0.3)
        body.put("max_tokens", 60)

        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "user").put("content", prompt))
        body.put("messages", msgs)

        val writer = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("HTTP " + code + "：" + text.take(120))

        val root = JSONObject(text)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) return emptyList()
        val content = choices.getJSONObject(0).getJSONObject("message").optString("content", "")

        return content
            .replace("\n", ",")
            .replace("，", ",")
            .replace("、", ",")
            .replace(".", "")
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.length < 20 && it.split(" ").size <= 2 }
            .distinct()
            .take(6)
    }
}
