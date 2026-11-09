package com.shilin.cuoci

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用 DeepSeek 现场生成写作干货。
 */
object StudyGen {

    private const val API_URL = "https://api.deepseek.com/chat/completions"

    data class Essay(val topic: String, val structure: String, val essay: String, val highlights: String)

    fun genReplacements(apiKey: String): List<StudyContent.Rep> {
        val prompt = "请给出 10 组英语四六级/考研写作的高分替换词，要实用、不要重复常见套路。\n" +
            "只输出一个 JSON 数组，不要任何解释，格式：\n" +
            "[{\"k\":\"普通表达（英文）\",\"kc\":\"中文意思\",\"v\":[\"高级替换1\",\"高级替换2\"],\"vc\":\"替换词的中文意思\"}]\n" +
            "注意 v 是字符串数组，2-3 个即可。"
        val arr = askJson(apiKey, prompt)
        val out = ArrayList<StudyContent.Rep>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val k = o.optString("k", "")
            if (k.isEmpty()) continue
            val vArr = o.optJSONArray("v")
            val vals = if (vArr != null) {
                val t = ArrayList<String>()
                for (j in 0 until vArr.length()) t.add(vArr.getString(j))
                t.joinToString(" / ")
            } else {
                o.optString("v", "")
            }
            out.add(
                StudyContent.Rep(
                    k,
                    o.optString("kc", "").replace("*", ""),
                    vals.replace("*", ""),
                    o.optString("vc", "").replace("*", "")
                )
            )
        }
        return out
    }

    fun genPatterns(apiKey: String): List<StudyContent.Pat> {
        val prompt = "请给出 8 个英语四六级/考研写作的万能句型，覆盖开头、主体、结尾。\n" +
            "只输出一个 JSON 数组，不要任何解释，格式：\n" +
            "[{\"tag\":\"开头/主体/结尾之一\",\"e\":\"英文句子（用 ... 占位）\",\"c\":\"中文翻译\"}]"
        val arr = askJson(apiKey, prompt)
        val out = ArrayList<StudyContent.Pat>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val e = o.optString("e", "").replace("*", "")
            if (e.isEmpty()) continue
            out.add(StudyContent.Pat(o.optString("tag", "句型"), e, o.optString("c", "").replace("*", "")))
        }
        return out
    }

    fun genEssay(apiKey: String, topic: String): Essay {
        val prompt = "请围绕「$topic」这个题材，写一篇英语四六级水平的考场范文。\n" +
            "要求：150-180 词，结构清晰（现象/观点 → 分析原因或论证 → 结论建议）。\n" +
            "只输出一个 JSON 对象，不要任何解释，格式：\n" +
            "{\"structure\":\"用三行概括三段各写什么\",\"essay\":\"英文范文正文\",\"highlights\":\"2-3 句可直接背的高分句型，每个后面用括号注明中文\"}"
        val obj = askObject(apiKey, prompt)
        return Essay(
            topic,
            obj.optString("structure", "").replace("*", ""),
            obj.optString("essay", "").replace("*", ""),
            obj.optString("highlights", "").replace("*", "")
        )
    }

    // ---------- 通用请求 ----------

    private fun askJson(apiKey: String, prompt: String): JSONArray {
        val content = request(apiKey, prompt)
        val s = content.indexOf('[')
        val e = content.lastIndexOf(']')
        if (s < 0 || e <= s) throw RuntimeException("返回格式不对")
        return JSONArray(content.substring(s, e + 1))
    }

    private fun askObject(apiKey: String, prompt: String): JSONObject {
        val content = request(apiKey, prompt)
        val s = content.indexOf('{')
        val e = content.lastIndexOf('}')
        if (s < 0 || e <= s) throw RuntimeException("返回格式不对")
        return JSONObject(content.substring(s, e + 1))
    }

    private fun request(apiKey: String, userPrompt: String): String {
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
        body.put("temperature", 1.0)
        body.put("max_tokens", 1200)
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
