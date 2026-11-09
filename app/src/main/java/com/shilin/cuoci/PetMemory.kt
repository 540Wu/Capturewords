package com.shilin.cuoci

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 宠物的对话记忆：把最近聊过的内容存下来，下次打开还认得你。
 */
object PetMemory {

    private const val FILE = "cuoci_pet_memory"
    private const val K_CHAT = "chat"
    private const val MAX_KEEP = 30

    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        sp = c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun save(list: List<Pair<Boolean, String>>) {
        val s = sp ?: return
        val keep = if (list.size > MAX_KEEP) list.subList(list.size - MAX_KEEP, list.size) else list
        val arr = JSONArray()
        for ((isUser, text) in keep) {
            val o = JSONObject()
            o.put("u", isUser)
            o.put("t", text)
            arr.put(o)
        }
        s.edit().putString(K_CHAT, arr.toString()).apply()
    }

    fun load(): List<Pair<Boolean, String>> {
        val s = sp ?: return emptyList()
        val raw = s.getString(K_CHAT, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Pair<Boolean, String>>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Pair(o.optBoolean("u", false), o.optString("t", "")))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        sp?.edit()?.remove(K_CHAT)?.apply()
    }

    fun count(): Int = load().size
}
