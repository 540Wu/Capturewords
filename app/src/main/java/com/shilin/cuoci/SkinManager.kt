package com.shilin.cuoci

import android.app.Activity
import android.content.Context
import android.util.TypedValue

/**
 * 外观皮肤管理。
 */
object SkinManager {

    const val DEFAULT = 0
    const val FARM = 1

    fun names(): Array<String> = arrayOf("默认（清爽蓝）", "星露谷（暖木色）")

    fun themeRes(skin: Int, sub: Boolean): Int {
        val s = if (skin > 1) 0 else skin
        return if (sub) {
            when (s) {
                FARM -> R.style.Theme_CaptureWords_Sub_Farm
                else -> R.style.Theme_CaptureWords_Sub
            }
        } else {
            when (s) {
                FARM -> R.style.Theme_CaptureWords_Farm
                else -> R.style.Theme_CaptureWords
            }
        }
    }

    /** 在 setContentView 之前调用。 */
    fun apply(activity: Activity, sub: Boolean = false) {
        activity.setTheme(themeRes(Prefs.skin(activity), sub))
    }

    /** 从当前主题里取颜色。 */
    fun color(ctx: Context, attr: Int): Int {
        val tv = TypedValue()
        ctx.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    fun textMain(ctx: Context): Int = color(ctx, R.attr.textMain)
    fun textSub(ctx: Context): Int = color(ctx, R.attr.textSub)
    fun textTertiary(ctx: Context): Int = color(ctx, R.attr.textTertiary)
    fun line(ctx: Context): Int = color(ctx, R.attr.lineColor)
    fun cardSolid(ctx: Context): Int = color(ctx, R.attr.cardSolid)
    fun brand(ctx: Context): Int = color(ctx, androidx.appcompat.R.attr.colorPrimary)

    fun colorOnBrand(ctx: Context): Int = color(ctx, com.google.android.material.R.attr.colorOnPrimary)
}
