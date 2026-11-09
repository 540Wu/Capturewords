package com.shilin.cuoci

import android.content.Context

/**
 * 宠物成长系统：
 * 经验 = 你在「不背单词」里学过的单词数量（按天增量累加）。
 * 等级越高，升级越慢。
 */
object PetStore {

    private const val FILE = "cuoci_pet"
    private const val K_EXP = "exp"
    private const val K_DAY = "day"
    private const val K_DAY_MAX = "day_max"
    private const val K_NAME = "name"
    private const val K_RENAME_TIP = "renamed"

    fun init(c: Context) {
        sp = c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    private var sp: android.content.SharedPreferences? = null

    /** 累计经验 */
    fun exp(): Int = sp?.getInt(K_EXP, 0) ?: 0

    /** 主人给它起的名字（可能为空） */
    fun petName(): String = sp?.getString(K_NAME, "") ?: ""

    fun setPetName(n: String) {
        sp?.edit()?.putString(K_NAME, n.trim())?.apply()
    }

    /** 展示用：有名字用名字，否则用形态名 */
    fun displayName(): String {
        val n = petName()
        return if (n.isNotEmpty()) n else name()
    }

    /** 今天新增的经验 */
    fun todayGain(): Int {
        val s = sp ?: return 0
        val day = s.getString(K_DAY, "") ?: ""
        if (day != WordStore.todayKey()) return 0
        return s.getInt(K_DAY_MAX, 0)
    }

    /**
     * 无障碍服务读到「不背单词」顶部的进度（如 42/173）后调用。
     * 同一天只累加“增加的部分”，切换词书导致数字变小也不会重复加。
     */
    fun onLearnedCount(learned: Int) {
        val s = sp ?: return
        if (learned <= 0) return
        val today = WordStore.todayKey()
        val day = s.getString(K_DAY, "") ?: ""
        var dayMax = s.getInt(K_DAY_MAX, 0)
        var total = s.getInt(K_EXP, 0)

        if (day != today) {
            // 新的一天，昨天的最大值作废
            dayMax = 0
        }

        if (learned > dayMax) {
            total += (learned - dayMax)
            dayMax = learned
            s.edit()
                .putInt(K_EXP, total)
                .putString(K_DAY, today)
                .putInt(K_DAY_MAX, dayMax)
                .apply()
        } else if (day != today) {
            s.edit().putString(K_DAY, today).putInt(K_DAY_MAX, dayMax).apply()
        }
    }

    /** 当前等级（从 1 开始）。 */
    fun level(): Int = levelOf(exp())

    fun levelOf(totalExp: Int): Int {
        var lv = 1
        var need = needFor(lv)
        var remain = totalExp
        while (remain >= need) {
            remain -= need
            lv++
            need = needFor(lv)
        }
        return lv
    }

    /** 升到下一级还需要多少经验。 */
    fun needFor(level: Int): Int = 30 + (level - 1) * 20

    /** 当前等级已经攒了多少（用于进度条）。 */
    fun progressInLevel(): Int {
        var remain = exp()
        var lv = 1
        var need = needFor(lv)
        while (remain >= need) {
            remain -= need
            lv++
            need = needFor(lv)
        }
        return remain
    }

    fun progressNeed(): Int = needFor(level())

    /** 形态：emoji + 名字 */
    fun emoji(): String = emojiOf(level())

    fun emojiOf(lv: Int): String = when (lv) {
        in 1..4 -> "🥚"
        in 5..9 -> "🐣"
        in 10..19 -> "🐥"
        in 20..34 -> "🐤"
        in 35..54 -> "🐔"
        in 55..79 -> "🦅"
        else -> "🐲"
    }

    fun name(): String = nameOf(level())

    fun nameOf(lv: Int): String = when (lv) {
        in 1..4 -> "一颗蛋"
        in 5..9 -> "刚破壳的小家伙"
        in 10..19 -> "毛茸茸的小鸡"
        in 20..34 -> "爱学习的小黄鸡"
        in 35..54 -> "神气的大公鸡"
        in 55..79 -> "展翅的雄鹰"
        else -> "小龙"
    }

    /** 从当前等级再攒多少经验能到目标等级。 */
    fun expToReach(target: Int): Int {
        val cur = level()
        if (cur >= target) return 0
        var need = needFor(cur) - progressInLevel()
        var lv = cur + 1
        while (lv < target) {
            need += needFor(lv)
            lv++
        }
        return need
    }

    /** 下一形态的门槛，用于提示。 */
    fun nextFormHint(): String {
        val lv = level()
        val target: Int
        val desc: String
        when {
            lv < 5 -> { target = 5; desc = "它会破壳" }
            lv < 10 -> { target = 10; desc = "它会长成小鸡" }
            lv < 20 -> { target = 20; desc = "它会长大一些" }
            lv < 35 -> { target = 35; desc = "它会变神气" }
            lv < 55 -> { target = 55; desc = "它会展翅" }
            lv < 80 -> { target = 80; desc = "它会化龙" }
            else -> return "已经在最高形态啦 ✨"
        }
        return "再背 " + expToReach(target) + " 个词升到 Lv." + target + "，" + desc
    }

    fun reset() {
        sp?.edit()?.clear()?.apply()
    }
}
