package com.shilin.cuoci

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    companion object {
        /** 生成结果放这里，避免用 Intent 传大对象。 */
        var parts: List<EssayGenerator.Part> = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        title = "今日短文"

        val container = findViewById<LinearLayout>(R.id.resultContainer)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)

        if (parts.isEmpty()) {
            tvSummary.text = "没有生成结果"
            return
        }

        val totalWords = parts.sumOf { it.words.size }
        val missingAll = parts.flatMap { it.missing }
        if (missingAll.isEmpty()) {
            tvSummary.text = "共 " + totalWords + " 个错词，分 " + parts.size + " 组 · ✅ 全部包含"
            tvSummary.setTextColor(0xFF1B7F28.toInt())
        } else {
            tvSummary.text = "共 " + totalWords + " 个错词，分 " + parts.size + " 组 · ⚠️ 未包含：" + missingAll.joinToString(", ")
            tvSummary.setTextColor(0xFFB3261E.toInt())
        }

        for ((index, p) in parts.withIndex()) {
            container.addView(buildCard(index + 1, p))
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, "错词捕手 · " + WordStore.todayKey())
            i.putExtra(Intent.EXTRA_TEXT, buildFullText())
            startActivity(Intent.createChooser(i, "分享到"))
        }

        findViewById<Button>(R.id.btnCopyAll).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("essay", buildFullText()))
            Toast.makeText(this, "全文已复制", Toast.LENGTH_SHORT).show()
        }
    }

    /** 拼出用于分享/复制的完整文本。 */
    private fun buildFullText(): String {
        val sb = StringBuilder()
        sb.append("【错词捕手 · ").append(WordStore.todayKey()).append("】\n\n")
        val allWords = parts.flatMap { it.words }
        sb.append("今日错词（").append(allWords.size).append("）：")
            .append(allWords.joinToString("、")).append("\n")

        for ((i, p) in parts.withIndex()) {
            sb.append("\n—— 短文 ").append(i + 1).append(" ——\n")
            sb.append(p.english).append("\n")
            if (p.chinese.isNotEmpty()) {
                sb.append("\n").append(p.chinese).append("\n")
            }
        }
        return sb.toString()
    }

    private fun buildCard(no: Int, p: EssayGenerator.Part): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(0, 0, 0, 28)

        val head = TextView(this)
        head.text = "短文 " + no + " · " + p.words.size + " 个词"
        head.textSize = 13f
        head.setTextColor(0xFF888888.toInt())
        card.addView(head)

        val en = TextView(this)
        en.text = highlight(p.english, p.words)
        en.textSize = 16f
        en.setTextColor(0xFF1A1A1A.toInt())
        en.setLineSpacing(10f, 1f)
        en.setTextIsSelectable(true)
        en.setPadding(0, 8, 0, 8)
        card.addView(en)

        val btn = Button(this)
        btn.text = "显示翻译"
        btn.textSize = 13f
        card.addView(btn)

        val zh = TextView(this)
        zh.text = if (p.chinese.isEmpty()) "（没有返回译文）" else p.chinese
        zh.textSize = 14f
        zh.setTextColor(0xFF555555.toInt())
        zh.setLineSpacing(8f, 1f)
        zh.setPadding(0, 8, 0, 8)
        zh.visibility = View.GONE
        card.addView(zh)

        btn.setOnClickListener {
            if (zh.visibility == View.GONE) {
                zh.visibility = View.VISIBLE
                btn.text = "隐藏翻译"
            } else {
                zh.visibility = View.GONE
                btn.text = "显示翻译"
            }
        }

        val divider = View(this)
        divider.setBackgroundColor(0xFFEEEEEE.toInt())
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1
        )
        card.addView(divider)

        return card
    }

    /** 把短文里的错词（含词形变化）加粗。 */
    private fun highlight(text: String, words: List<String>): CharSequence {
        val ss = SpannableString(text)
        val lower = text.lowercase()
        for (w in words) {
            val root = EssayGenerator.stem(w)
            if (root.isEmpty()) continue
            var idx = lower.indexOf(root)
            while (idx >= 0) {
                ss.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    idx, idx + root.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                idx = lower.indexOf(root, idx + root.length)
            }
        }
        return ss
    }
}
