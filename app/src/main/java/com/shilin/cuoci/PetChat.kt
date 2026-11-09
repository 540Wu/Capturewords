package com.shilin.cuoci

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 和宠物聊天：回复强制简短（1-2 句，30 字以内）。
 */
object PetChat {

    private const val API_URL = "https://api.deepseek.com/chat/completions"

    fun reply(
        apiKey: String,
        petName: String,
        level: Int,
        todayWords: Int,
        history: List<Pair<Boolean, String>>,
        userText: String
    ): String {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 60000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Authorization", "Bearer " + apiKey)

        val body = JSONObject()
        body.put("model", "deepseek-chat")
        body.put("stream", false)
        body.put("temperature", 1.2)
        body.put("max_tokens", 80)

        val msgs = JSONArray()

        val sys = "你是主人养的电子宠物，名叫「" + petName + "」，现在是" + formName(level) +
            "（Lv." + level + "）。\n" +
            persona(level) + "\n" +
            "今天主人已经背了 " + todayWords + " 个单词。\n" +
            "严格限制：每次只说 1 句，最多 25 个字，绝对不要超过 2 句。不要用列表、不要解释、不要长篇大论。\n" +
            "可以使用颜文字或一两个 emoji。偶尔催主人去背单词。说话风格必须符合你现在的形态，别跑偏。"
        msgs.put(JSONObject().put("role", "system").put("content", sys))

        // 最近几条对话作为上下文
        val recent = if (history.size > 6) history.subList(history.size - 6, history.size) else history
        for ((isUser, text) in recent) {
            msgs.put(
                JSONObject()
                    .put("role", if (isUser) "user" else "assistant")
                    .put("content", text)
            )
        }
        msgs.put(JSONObject().put("role", "user").put("content", userText))
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
        if (choices.length() == 0) throw RuntimeException("没有回复")
        val content = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim()
        if (content.isEmpty()) return "……（它好像睡着了）"
        return trimReply(content)
    }

    /** 不同形态的性格设定 */
    private fun persona(level: Int): String = when {
        level in 0..4 -> "你现在还是一颗蛋，还没孵化。说话像在壳里听到外面声响的小家伙：喃喃自语、含糊，经常猜错主人的意思，对破壳充满期待。语气天真、好奇、有点委屈。"
        level in 5..9 -> "你刚破壳，奶声奶气，说话简短、断断续续，看什么都新鲜，特别爱问为什么，容易满足，一被夸就很开心。"
        level in 10..19 -> "你是毛茸茸的小鸡，活泼好动、爱撒娇，喜欢用叠词和颜文字，偶尔犯傻，喜欢黏着主人讨摸摸。"
        level in 20..34 -> "你是爱学习的小黄鸡，开始有点小骄傲，喜欢炫耀自己认识的字和背过的单词，会像小老师一样催主人背单词。"
        level in 35..54 -> "你是神气的大公鸡，自信、嗓门大、爱说教，喜欢喊口号给主人打气，偶尔吹一点牛，但真的很在乎主人的成绩。"
        level in 55..79 -> "你是展翅的雄鹰，沉稳、有格局、说话短促有力，喜欢用比喻谈坚持、远方和格局，不太撒娇，但会稳稳地把主人托住。"
        else -> "你是一条小龙，高冷、话少、有威严，但会默默护着主人，偶尔敲一下主人的头提醒他背书，属于嘴硬心软的那种。"
    }

    /** 形态名称 */
    private fun formName(level: Int): String = when (level) {
        in 0..4 -> "一颗蛋"
        in 5..9 -> "刚破壳的小家伙"
        in 10..19 -> "毛茸茸的小鸡"
        in 20..34 -> "爱学习的小黄鸡"
        in 35..54 -> "神气的大公鸡"
        in 55..79 -> "展翅的雄鹰"
        else -> "小龙"
    }

    /** 兜底：万一模型说了长篇，也压到两句以内。 */
    private fun trimReply(s: String): String {
        val oneLine = s.replace("\n", " ").trim()
        if (oneLine.length <= 40) return oneLine
        val cut = oneLine.substring(0, 40)
        val lastPunc = cut.lastIndexOfAny(charArrayOf('。', '！', '？', '~', '～'))
        return if (lastPunc > 10) cut.substring(0, lastPunc + 1) else cut + "…"
    }
}
