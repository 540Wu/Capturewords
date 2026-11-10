package com.shilin.cuoci

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 易混淆词：根据今天的错词生成形近/近义易混词。
 */
class ConfuseActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private lateinit var tvHint: TextView
    private lateinit var btnRegen: Button
    private var generating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confuse)
        title = "易混淆词"
        WordStore.init(this)

        list = findViewById(R.id.listConfuse)
        tvHint = findViewById(R.id.tvConfuseHint)
        btnRegen = findViewById(R.id.btnRegenConfuse)

        btnRegen.setOnClickListener { generate() }

        // 今天已经生成过就直接用缓存，不重复烧 token
        val day = WordStore.todayKey()
        val cached = Prefs.confuseCache(this, day)
        if (cached.isNotEmpty()) {
            val items = ConfusableGen.fromJson(cached)
            if (items.isNotEmpty()) {
                tvHint.text = "今天已经生成过（点下面可以重新生成）"
                btnRegen.text = "换一批（AI 重新生成）"
                render(items)
                return
            }
        }
        generate()
    }

    private fun generate() {
        if (generating) return
        val words = WordStore.today().map { it.word }.take(15)
        if (words.isEmpty()) {
            tvHint.text = "今天还没有错词"
            list.removeAllViews()
            return
        }

        generating = true
        btnRegen.isEnabled = false
        btnRegen.text = "生成中…"

        val key = Prefs.apiKey(this)

        Thread {
            var items: List<ConfusableGen.Item>? = null
            var aiWorked = false
            if (key.isNotEmpty()) {
                try {
                    items = ConfusableGen.ai(key, words)
                    aiWorked = true
                } catch (e: Exception) {
                    items = null
                }
            }
            if (items == null) {
                items = ConfusableGen.local(words)
            }

            val result = items
            val usedAi = aiWorked
            if (usedAi) {
                // 缓存结果，避免重复生成
                Prefs.setConfuseCache(this, WordStore.todayKey(), ConfusableGen.toJson(result))
            }
            handler.post {
                generating = false
                btnRegen.isEnabled = true
                btnRegen.text = if (usedAi) "换一批（AI 重新生成）" else "填 API Key 后可 AI 生成"
                tvHint.text = if (usedAi)
                    "根据今天的 " + words.size + " 个错词，AI 找出的易混淆词"
                else
                    "没填 API Key，先按拼写相似度帮你比对（填 Key 后能分析意思和发音）"
                render(result)
            }
        }.start()
    }

    private fun render(items: List<ConfusableGen.Item>) {
        list.removeAllViews()
        val ctx = this
        val brand = SkinManager.brand(ctx)

        for ((index, it) in items.withIndex()) {
            if (index > 0) {
                val div = android.view.View(ctx)
                div.setBackgroundColor(SkinManager.line(ctx))
                div.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                list.addView(div)
            }

            val word = TextView(ctx)
            word.text = it.word
            word.textSize = 19f
            word.setTypeface(null, Typeface.BOLD)
            word.setTextColor(brand)
            word.setPadding(0, dp(14), 0, dp(4))
            list.addView(word)

            val conf = TextView(ctx)
            conf.text = if (it.confusables.isEmpty()) "（没有找到相似的词）"
            else "易混： " + it.confusables.joinToString("  /  ")
            conf.textSize = 15f
            conf.setTextColor(SkinManager.textMain(ctx))
            conf.setLineSpacing(dp(3).toFloat(), 1f)
            list.addView(conf)

            val tip = TextView(ctx)
            tip.text = it.tip
            tip.textSize = 13f
            tip.setTextColor(SkinManager.textSub(ctx))
            tip.setPadding(0, dp(4), 0, dp(14))
            tip.setLineSpacing(dp(3).toFloat(), 1f)
            list.addView(tip)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
