package com.shilin.cuoci

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * 写作干货：替换词 / 万能句型 / 按题材生成范文，都可以让 AI 重新生成。
 */
class StudyFragment : Fragment() {

    private var list: LinearLayout? = null
    private var tabReplace: TextView? = null
    private var tabPattern: TextView? = null
    private var tabTemplate: TextView? = null
    private var topicRow: HorizontalScrollView? = null
    private var topicContainer: LinearLayout? = null
    private var btnRegen: Button? = null

    private var current = 0
    private var selectedTopic = 0

    private var reps: List<StudyContent.Rep> = StudyContent.replacements
    private var pats: List<StudyContent.Pat> = StudyContent.patterns
    private var essay: StudyGen.Essay? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_study, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list = view.findViewById(R.id.listStudy)
        tabReplace = view.findViewById(R.id.tabReplace)
        tabPattern = view.findViewById(R.id.tabPattern)
        tabTemplate = view.findViewById(R.id.tabTemplate)
        topicRow = view.findViewById(R.id.topicRow)
        topicContainer = view.findViewById(R.id.topicContainer)
        btnRegen = view.findViewById(R.id.btnRegen)

        tabReplace?.setOnClickListener { current = 0; render() }
        tabPattern?.setOnClickListener { current = 1; render() }
        tabTemplate?.setOnClickListener { current = 2; render() }

        btnRegen?.setOnClickListener { regenerate() }

        render()
    }

    private fun render() {
        val ctx = context ?: return
        val box = list ?: return
        box.removeAllViews()
        (list?.parent as? ScrollView)?.scrollTo(0, 0)

        val on = SkinManager.brand(ctx)
        val off = SkinManager.textTertiary(ctx)
        styleTab(tabReplace, current == 0, on, off)
        styleTab(tabPattern, current == 1, on, off)
        styleTab(tabTemplate, current == 2, on, off)

        topicRow?.visibility = if (current == 2) View.VISIBLE else View.GONE
        btnRegen?.text = when (current) {
            0 -> "换一批新的替换词（AI）"
            1 -> "换一批新的句型（AI）"
            else -> "生成这个题材的范文（AI）"
        }

        when (current) {
            0 -> renderReplacements(box, on)
            1 -> renderPatterns(box, on)
            else -> {
                renderTopics(box)
                renderEssay(box, on)
            }
        }
    }

    private fun renderReplacements(box: LinearLayout, on: Int) {
        val ctx = context ?: return
        for ((i, r) in reps.withIndex()) {
            if (i > 0) box.addView(divider())

            val key = TextView(ctx)
            key.text = r.key + "（" + r.keyCn + "）"
            key.textSize = 14f
            key.setTypeface(null, Typeface.BOLD)
            key.setTextColor(on)
            key.setPadding(0, dp(12), 0, dp(2))
            box.addView(key)

            val valTv = TextView(ctx)
            valTv.text = "→ " + r.vals
            valTv.textSize = 14f
            valTv.setTextColor(SkinManager.textMain(ctx))
            valTv.setLineSpacing(dp(3).toFloat(), 1f)
            box.addView(valTv)

            if (r.valsCn.isNotEmpty()) {
                val cn = TextView(ctx)
                cn.text = "　 " + r.valsCn
                cn.textSize = 12f
                cn.setTextColor(SkinManager.textSub(ctx))
                cn.setPadding(0, dp(2), 0, dp(10))
                box.addView(cn)
            } else {
                val pad = View(ctx)
                pad.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8)
                )
                box.addView(pad)
            }
        }
    }

    private fun renderPatterns(box: LinearLayout, on: Int) {
        val ctx = context ?: return
        for ((i, p) in pats.withIndex()) {
            if (i > 0) box.addView(divider())

            val tag = TextView(ctx)
            tag.text = "【" + p.tag + "】"
            tag.textSize = 12f
            tag.setTextColor(on)
            tag.setPadding(0, dp(12), 0, dp(2))
            box.addView(tag)

            val en = TextView(ctx)
            en.text = p.en
            en.textSize = 15f
            en.setTextColor(SkinManager.textMain(ctx))
            en.setLineSpacing(dp(3).toFloat(), 1f)
            box.addView(en)

            val cn = TextView(ctx)
            cn.text = p.cn
            cn.textSize = 13f
            cn.setTextColor(SkinManager.textSub(ctx))
            cn.setPadding(0, dp(4), 0, dp(10))
            cn.setLineSpacing(dp(2).toFloat(), 1f)
            box.addView(cn)
        }
    }

    private fun renderTopics(box: LinearLayout) {
        val ctx = context ?: return
        val row = topicContainer ?: return
        row.removeAllViews()

        val on = SkinManager.brand(ctx)
        val off = SkinManager.textSub(ctx)

        for ((i, t) in StudyContent.topics.withIndex()) {
            val chip = TextView(ctx)
            chip.text = t
            chip.textSize = 12f
            chip.setPadding(dp(12), dp(7), dp(12), dp(7))
            chip.setTextColor(if (i == selectedTopic) on else off)
            chip.setTypeface(null, if (i == selectedTopic) Typeface.BOLD else Typeface.NORMAL)
            chip.setBackgroundResource(
                if (i == selectedTopic) R.drawable.bg_soft else 0
            )
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.rightMargin = dp(8)
            chip.layoutParams = lp
            chip.setOnClickListener {
                selectedTopic = i
                render()
            }
            row.addView(chip)
        }
    }

    private fun renderEssay(box: LinearLayout, on: Int) {
        val ctx = context ?: return
        val e = essay
        if (e == null) {
            val tip = TextView(ctx)
            tip.text = "选一个题材，点下面的按钮，让 AI 现写一篇 150-180 词的范文。\n\n" +
                "生成的范文包含：结构拆解 + 英文正文 + 可直接背的高分句型。"
            tip.textSize = 13f
            tip.setTextColor(SkinManager.textSub(ctx))
            tip.setLineSpacing(dp(4).toFloat(), 1f)
            tip.setPadding(0, dp(8), 0, dp(8))
            box.addView(tip)
            return
        }

        val title = TextView(ctx)
        title.text = "题材：" + e.topic
        title.textSize = 15f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(on)
        title.setPadding(0, 0, 0, dp(10))
        box.addView(title)

        if (e.structure.isNotEmpty()) {
            box.addView(sectionLabel("结构"))
            val tv = TextView(ctx)
            tv.text = e.structure
            tv.textSize = 13f
            tv.setTextColor(SkinManager.textSub(ctx))
            tv.setLineSpacing(dp(4).toFloat(), 1f)
            tv.setPadding(0, dp(4), 0, dp(12))
            box.addView(tv)
        }

        box.addView(sectionLabel("范文"))
        val essayTv = TextView(ctx)
        essayTv.text = e.essay
        essayTv.textSize = 15f
        essayTv.setTextColor(SkinManager.textMain(ctx))
        essayTv.setLineSpacing(dp(6).toFloat(), 1f)
        essayTv.setPadding(0, dp(6), 0, dp(14))
        essayTv.setTextIsSelectable(true)
        box.addView(essayTv)

        if (e.highlights.isNotEmpty()) {
            box.addView(sectionLabel("高分句型"))
            val hl = TextView(ctx)
            hl.text = e.highlights
            hl.textSize = 13f
            hl.setTextColor(SkinManager.textSub(ctx))
            hl.setLineSpacing(dp(5).toFloat(), 1f)
            hl.setPadding(0, dp(6), 0, dp(6))
            box.addView(hl)
        }
    }

    private fun sectionLabel(t: String): TextView {
        val ctx = context!!
        val tv = TextView(ctx)
        tv.text = t
        tv.textSize = 12f
        tv.setTypeface(null, Typeface.BOLD)
        tv.setTextColor(SkinManager.brand(ctx))
        tv.setPadding(0, dp(8), 0, 0)
        return tv
    }

    private fun regenerate() {
        val ctx = context ?: return
        val key = Prefs.apiKey(ctx)
        if (key.isEmpty()) {
            Toast.makeText(ctx, "先去设置里填 DeepSeek API Key", Toast.LENGTH_LONG).show()
            startActivity(Intent(ctx, SettingsActivity::class.java))
            return
        }

        val btn = btnRegen ?: return
        btn.isEnabled = false
        val oldText = btn.text.toString()
        btn.text = "生成中…"

        val tab = current
        val topic = StudyContent.topics[selectedTopic]

        Thread {
            try {
                when (tab) {
                    0 -> {
                        val r = StudyGen.genReplacements(key)
                        handler.post {
                            if (!isAdded) return@post
                            reps = r
                            btn.isEnabled = true
                            btn.text = oldText
                            render()
                        }
                    }
                    1 -> {
                        val p = StudyGen.genPatterns(key)
                        handler.post {
                            if (!isAdded) return@post
                            pats = p
                            btn.isEnabled = true
                            btn.text = oldText
                            render()
                        }
                    }
                    else -> {
                        val e = StudyGen.genEssay(key, topic)
                        handler.post {
                            if (!isAdded) return@post
                            essay = e
                            btn.isEnabled = true
                            btn.text = oldText
                            render()
                        }
                    }
                }
            } catch (ex: Exception) {
                handler.post {
                    if (!isAdded) return@post
                    btn.isEnabled = true
                    btn.text = oldText
                    Toast.makeText(ctx, "生成失败：" + ex.message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun divider(): View {
        val ctx = requireContext()
        val div = View(ctx)
        div.setBackgroundColor(SkinManager.line(ctx))
        div.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        return div
    }

    private fun styleTab(tv: TextView?, active: Boolean, on: Int, off: Int) {
        tv ?: return
        tv.setTextColor(if (active) on else off)
        tv.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
