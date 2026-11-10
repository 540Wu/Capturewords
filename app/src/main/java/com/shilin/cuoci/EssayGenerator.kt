package com.shilin.cuoci

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * 调用 DeepSeek：把错词按每组 8 个分组，每组生成一篇 50-100 词短文 + 中文译文。
 */
object EssayGenerator {

    private const val API_URL = "https://api.deepseek.com/chat/completions"
    const val GROUP_SIZE = 8

    private const val SYSTEM_PROMPT =
        "你是一位英语写作老师，擅长把学生记错的单词自然融入短小精悍的英语短文。"

    class Part(
        val words: List<String>,
        val english: String,
        val chinese: String,
        val missing: List<String>
    )

    fun generateAll(
        apiKey: String,
        words: List<String>,
        onProgress: (done: Int, total: Int) -> Unit
    ): List<Part> {
        val groups = words.chunked(GROUP_SIZE)
        val out = arrayOfNulls<Part>(groups.size)
        val errs = arrayOfNulls<String>(groups.size)
        val latch = CountDownLatch(groups.size)
        val done = AtomicInteger(0)

        for ((i, g) in groups.withIndex()) {
            Thread {
                try {
                    out[i] = generateOne(apiKey, g)
                } catch (e: Exception) {
                    errs[i] = e.message
                } finally {
                    onProgress(done.incrementAndGet(), groups.size)
                    latch.countDown()
                }
            }.start()
        }
        latch.await()

        val ok = out.filterNotNull()
        if (ok.isEmpty()) {
            val firstErr = errs.firstOrNull { it != null } ?: "未知错误"
            throw RuntimeException(firstErr)
        }
        // 按原顺序返回，并保留失败组的信息
        return groups.indices.mapNotNull { out[it] }
    }

    private fun generateOne(apiKey: String, words: List<String>): Part {
        val raw = post(apiKey, buildPrompt(words))
        val content = parse(raw)
        val pair = split(content)
        val missing = words.filter { !appears(it, pair.first) }
        return Part(words, pair.first, pair.second, missing)
    }

    private fun buildPrompt(words: List<String>): String {
        val sb = StringBuilder()
        sb.append("请用下面这些单词写一篇英语短文，要求：\n")
        sb.append("1. 长度 50-100 词，难度接近考研英语阅读；\n")
        sb.append("2. 必须自然地用到每一个单词（可以用它们的其他形式，例如复数、过去式、名词化），每个至少出现一次；\n")
        sb.append("3. 内容连贯、有明确主题，不要写成造句罗列；\n")
        sb.append("4. 不要在单词上加任何标记符号（不要用星号、下划线、加粗等）；\n")
        sb.append("5. 严格按下面的格式输出，不要输出其他内容：\n")
        sb.append("[ENGLISH]\n英文短文\n[CHINESE]\n中文译文\n\n")
        sb.append("单词列表：")
        sb.append(words.joinToString(", "))
        return sb.toString()
    }

    private fun post(apiKey: String, userPrompt: String): String {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 120000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer " + apiKey)

        val body = JSONObject()
        body.put("model", "deepseek-chat")
        body.put("stream", false)
        body.put("temperature", 1.1)
        // 50~100 词短文 + 中文译文，400 token 足够
        body.put("max_tokens", 420)
        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
        msgs.put(JSONObject().put("role", "user").put("content", userPrompt))
        body.put("messages", msgs)

        val writer = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            throw RuntimeException("HTTP " + code + "：" + text.take(200))
        }
        return text
    }

    private fun parse(resp: String): String {
        val root = JSONObject(resp)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) throw RuntimeException("返回里没有内容")
        val msg = choices.getJSONObject(0).getJSONObject("message")
        val content = msg.optString("content", "")
        if (content.isBlank()) throw RuntimeException("模型返回了空内容")
        return content.trim()
    }

    /** 拆出英文和中文。 */
    private fun split(content: String): Pair<String, String> {
        val marker = "[CHINESE]"
        val i = content.indexOf(marker, ignoreCase = true)
        if (i < 0) {
            return Pair(content.replace("[ENGLISH]", "").trim(), "")
        }
        val en = content.substring(0, i).replace("[ENGLISH]", "").trim()
        val zh = content.substring(i + marker.length).trim()
        return Pair(en, zh)
    }

    /** 取词根，用于宽松匹配（magnetic -> magnet）。 */
    fun stem(word: String): String {
        val w = word.lowercase().replace("·", "").replace("-", "").replace(" ", "").trim()
        if (w.isEmpty()) return w
        return if (w.length <= 5) w else w.substring(0, w.length - 2)
    }

    fun appears(word: String, text: String): Boolean {
        val root = stem(word)
        if (root.isEmpty()) return true
        return text.lowercase().contains(root)
    }
}
