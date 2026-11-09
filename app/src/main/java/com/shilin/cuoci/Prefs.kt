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
}
