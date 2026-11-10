package com.shilin.cuoci

/**
 * 词形变化（完全本地计算，不花 token）。
 */
object WordForm {

    class Analysis(val base: String, val forms: List<Pair<String, String>>)

    fun analyze(word: String): Analysis {
        val w = word.lowercase().trim().replace("·", "")
        val base = findBase(w)
        val root = base ?: w

        val forms = ArrayList<Pair<String, String>>()
        plural(root).let { if (it != root) forms.add("复数/三单" to it) }
        ing(root).let { if (it != root) forms.add("现在分词" to it) }
        past(root).let { if (it != root) forms.add("过去式" to it) }
        comparative(root).let { if (it != root) forms.add("比较级" to it) }
        superlative(root).let { if (it != root) forms.add("最高级" to it) }

        return Analysis(base ?: "", forms)
    }

    /** 如果这个词本身是变形，找回原型（必须能在词库里查到才算） */
    private fun findBase(w: String): String? {
        val rules = listOf(
            "ies" to "y",
            "ied" to "y",
            "ier" to "y",
            "iest" to "y",
            "es" to "",
            "s" to "",
            "ed" to "",
            "ing" to "",
            "ing" to "e",
            "er" to "",
            "est" to "",
            "d" to "",
            "er" to "e",
            "est" to "e"
        )
        for ((suf, add) in rules) {
            if (!w.endsWith(suf)) continue
            var base = w.substring(0, w.length - suf.length) + add
            if (base.length < 3) continue
            if (WordBank.lookup(base) != null) return base
            // 双写还原：stopped -> stop
            if (base.length > 3) {
                val c = base[base.length - 1]
                if (base[base.length - 2] == c && "bdgmnprt".contains(c)) {
                    val single = base.substring(0, base.length - 1)
                    if (WordBank.lookup(single) != null) return single
                }
            }
        }
        return null
    }

    private fun plural(w: String): String {
        val es = listOf("s", "x", "z", "ch", "sh", "o")
        return when {
            w.endsWith("y") && w.length > 1 && !isVowel(w[w.length - 2]) -> w.substring(0, w.length - 1) + "ies"
            es.any { w.endsWith(it) } -> w + "es"
            else -> w + "s"
        }
    }

    private fun ing(w: String): String {
        return when {
            w.endsWith("ie") -> w.substring(0, w.length - 2) + "ying"
            w.endsWith("e") && !w.endsWith("ee") && !w.endsWith("ye") -> w.substring(0, w.length - 1) + "ing"
            shouldDouble(w) -> w + w.last() + "ing"
            else -> w + "ing"
        }
    }

    private fun past(w: String): String {
        return when {
            w.endsWith("e") -> w + "d"
            w.endsWith("y") && w.length > 1 && !isVowel(w[w.length - 2]) -> w.substring(0, w.length - 1) + "ied"
            shouldDouble(w) -> w + w.last() + "ed"
            else -> w + "ed"
        }
    }

    private fun comparative(w: String): String {
        return when {
            w.endsWith("e") -> w + "r"
            w.endsWith("y") && w.length > 1 && !isVowel(w[w.length - 2]) -> w.substring(0, w.length - 1) + "ier"
            shouldDouble(w) -> w + w.last() + "er"
            else -> w + "er"
        }
    }

    private fun superlative(w: String): String {
        return when {
            w.endsWith("e") -> w + "st"
            w.endsWith("y") && w.length > 1 && !isVowel(w[w.length - 2]) -> w.substring(0, w.length - 1) + "iest"
            shouldDouble(w) -> w + w.last() + "est"
            else -> w + "est"
        }
    }

    /** 单音节 CVC 结尾需要双写（stop -> stopped） */
    private fun shouldDouble(w: String): Boolean {
        if (w.length < 3) return false
        val a = w[w.length - 1]
        val b = w[w.length - 2]
        val c = w[w.length - 3]
        return !isVowel(a) && isVowel(b) && !isVowel(c) && "bdgmnprt".contains(a)
    }

    private fun isVowel(c: Char): Boolean = "aeiou".contains(c)
}
