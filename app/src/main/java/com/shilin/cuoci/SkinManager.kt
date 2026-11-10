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
    const val DARK = 2
    const val MINT = 3
    const val SUNSET = 4

    fun names(): Array<String> = arrayOf(
        "默认（清爽蓝）",
        "星露谷（暖木色）",
        "暗夜紫（深色）",
        "薄荷绿（清新）",
        "暖阳橙（活力）"
    )

    fun themeRes(skin: Int, sub: Boolean): Int {
        val s = if (skin > 4 || skin < 0) 0 else skin
        return if (sub) {
            when (s) {
                FARM -> R.style.Theme_CaptureWords_Sub_Farm
                DARK -> R.style.Theme_CaptureWords_Sub_Dark
                MINT -> R.style.Theme_CaptureWords_Sub_Mint
                SUNSET -> R.style.Theme_CaptureWords_Sub_Sunset
                else -> R.style.Theme_CaptureWords_Sub
            }
        } else {
            when (s) {
                FARM -> R.style.Theme_CaptureWords_Farm
                DARK -> R.style.Theme_CaptureWords_Dark
                MINT -> R.style.Theme_CaptureWords_Mint
                SUNSET -> R.style.Theme_CaptureWords_Sunset
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
