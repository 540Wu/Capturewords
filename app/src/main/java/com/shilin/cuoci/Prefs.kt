package com.shilin.cuoci

import android.content.Context

object Prefs {
    private const val FILE = "cuoci_prefs"
    private const val KEY_API = "api_key"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun apiKey(ctx: Context): String = sp(ctx).getString(KEY_API, "") ?: ""

    fun setApiKey(ctx: Context, key: String) {
        sp(ctx).edit().putString(KEY_API, key.trim()).apply()
    }

    fun skin(ctx: Context): Int = sp(ctx).getInt("skin", 0)

    fun setSkin(ctx: Context, s: Int) {
        sp(ctx).edit().putInt("skin", s).apply()
    }

    fun tipDay(ctx: Context): String = sp(ctx).getString("tip_day", "") ?: ""

    fun setTipDay(ctx: Context, day: String) {
        sp(ctx).edit().putString("tip_day", day).apply()
    }

    // 易混淆词缓存（按天）
    fun confuseCache(ctx: Context, day: String): String =
        sp(ctx).getString("confuse_" + day, "") ?: ""

    fun setConfuseCache(ctx: Context, day: String, json: String) {
        sp(ctx).edit().putString("confuse_" + day, json).apply()
    }

    // 宠物每天聊天条数（防止一直聊烧 token）
    fun petChatCount(ctx: Context): Int {
        val s = sp(ctx)
        if (s.getString("chat_day", "") != WordStore.todayKey()) return 0
        return s.getInt("chat_count", 0)
    }

    fun incPetChat(ctx: Context) {
        sp(ctx).edit()
            .putString("chat_day", WordStore.todayKey())
            .putInt("chat_count", petChatCount(ctx) + 1)
            .apply()
    }

    // 上次同步成功的设备地址
    fun lanIp(ctx: Context): String = sp(ctx).getString("lan_ip", "") ?: ""

    fun setLanIp(ctx: Context, ip: String) {
        sp(ctx).edit().putString("lan_ip", ip.trim()).apply()
    }

    // 每个词的近义词缓存（同一个词只花一次 token）
    fun synonym(ctx: Context, word: String): String =
        sp(ctx).getString("syn_" + word.lowercase(), "") ?: ""

    fun setSynonym(ctx: Context, word: String, v: String) {
        sp(ctx).edit().putString("syn_" + word.lowercase(), v).apply()
    }
}
