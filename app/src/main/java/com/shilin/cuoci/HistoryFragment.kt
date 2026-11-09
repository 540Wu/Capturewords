package com.shilin.cuoci

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class HistoryFragment : Fragment() {

    private var ym: YearMonth = YearMonth.now()
    private var selected: LocalDate = LocalDate.now()

    private var tvMonth: TextView? = null
    private var weekContainer: LinearLayout? = null
    private var detailContainer: LinearLayout? = null
    private var tts: TextToSpeech? = null

    private val colorMarkedBg: Int
        get() = (SkinManager.brand(requireContext()) and 0x00FFFFFF) or 0x22000000
    private val colorSelectedBg: Int
        get() = SkinManager.brand(requireContext())
    private val colorAccent: Int
        get() = SkinManager.brand(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvMonth = view.findViewById(R.id.tvMonth)
        weekContainer = view.findViewById(R.id.weekContainer)
        detailContainer = view.findViewById(R.id.detailContainer)

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        view.findViewById<TextView>(R.id.btnPrevMonth).setOnClickListener {
            ym = ym.minusMonths(1)
            selected = defaultDay()
            render()
        }
        view.findViewById<TextView>(R.id.btnNextMonth).setOnClickListener {
            ym = ym.plusMonths(1)
            selected = defaultDay()
            render()
        }

        selected = defaultDay()
        render()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) render()
    }

    override fun onDestroyView() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroyView()
    }

    private fun defaultDay(): LocalDate {
        val marks = markedDays()
        if (ym == YearMonth.now()) return LocalDate.now()
        if (marks.isEmpty()) return ym.atDay(1)
        return marks.maxOrNull()?.let { ym.atDay(it) } ?: ym.atDay(1)
    }

    private fun markedDays(): Set<Int> {
        val prefix = ym.toString()
        return WordStore.days()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substring(8).toIntOrNull() }
            .toSet()
    }

    private fun render() {
        tvMonth?.text = ym.year.toString() + " 年 " + ym.monthValue + " 月"
        renderCalendar()
        renderDetail()
    }

    private fun renderCalendar() {
        val box = weekContainer ?: return
        box.removeAllViews()
        val marks = markedDays()

        box.addView(buildRow(listOf("一", "二", "三", "四", "五", "六", "日"), true, marks))

        val first = ym.atDay(1)
        val offset = (first.dayOfWeek.value + 6) % 7
        val len = ym.lengthOfMonth()

        var day = 1
        var cellIndex = 0
        while (day <= len) {
            val cells = ArrayList<String>(7)
            for (i in 0 until 7) {
                if (cellIndex < offset || day > len) {
                    cells.add("")
                } else {
                    cells.add(day.toString())
                    day++
                }
                cellIndex++
            }
            box.addView(buildRow(cells, false, marks))
        }
    }

    private fun buildRow(cells: List<String>, header: Boolean, marks: Set<Int>): LinearLayout {
        val row = LinearLayout(requireContext())
        row.orientation = LinearLayout.HORIZONTAL

        for (c in cells) {
            val tv = TextView(requireContext())
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tv.gravity = Gravity.CENTER
            tv.setPadding(0, dp(10), 0, dp(10))
            tv.textSize = if (header) 12f else 15f
            tv.text = c

            if (header) {
                tv.setTextColor(SkinManager.textTertiary(requireContext()))
            } else if (c.isEmpty()) {
                tv.text = ""
            } else {
                val d = c.toInt()
                val date = ym.atDay(d)
                val hasData = marks.contains(d)
                val isSelected = date == selected
                val isToday = date == LocalDate.now()

                when {
                    isSelected -> {
                        tv.setBackgroundColor(colorSelectedBg)
                        tv.setTextColor(Color.WHITE)
                        tv.setTypeface(null, Typeface.BOLD)
                    }
                    hasData -> {
                        tv.setBackgroundColor(colorMarkedBg)
                        tv.setTextColor(colorAccent)
                        tv.setTypeface(null, Typeface.BOLD)
                    }
                    isToday -> {
                        tv.setTextColor(colorAccent)
                        tv.setTypeface(null, Typeface.BOLD)
                    }
                    else -> {
                        tv.setTextColor(SkinManager.textMain(requireContext()))
                    }
                }

                tv.setOnClickListener {
                    selected = date
                    render()
                }
            }
            row.addView(tv)
        }
        return row
    }

    private fun renderDetail() {
        val box = detailContainer ?: return
        box.removeAllViews()
        val dayKey = selected.toString()
        val list = WordStore.of(dayKey)
        val essay = EssayStore.get(dayKey)

        val head = LinearLayout(requireContext())
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(0, dp(16), 0, dp(8))

        val tvDay = TextView(requireContext())
        tvDay.text = dayKey + " · " + list.size + " 个错词"
        tvDay.textSize = 15f
        tvDay.setTextColor(SkinManager.textMain(requireContext()))
        tvDay.setTypeface(null, Typeface.BOLD)
        tvDay.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        head.addView(tvDay)

        if (essay != null && essay.parts.isNotEmpty()) {
            val btn = Button(requireContext())
            btn.text = "看短文"
            btn.textSize = 12f
            btn.setOnClickListener {
                ResultActivity.parts = essay.parts
                startActivity(Intent(requireContext(), ResultActivity::class.java))
            }
            head.addView(btn)
        }
        box.addView(head)

        if (list.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = "这天没有记录。"
            tv.setTextColor(SkinManager.textSub(requireContext()))
            tv.textSize = 13f
            tv.setPadding(dp(4), dp(6), 0, dp(6))
            box.addView(tv)
            return
        }

        for (r in list) {
            val tv = TextView(requireContext())
            tv.text = "· " + r.word + if (r.meaning.isEmpty()) "" else "  " + r.meaning
            tv.textSize = 16f
            tv.setTextColor(SkinManager.textMain(requireContext()))
            tv.setPadding(dp(6), dp(6), 0, dp(6))
            tv.maxLines = 1
            tv.ellipsize = android.text.TextUtils.TruncateAt.END
            tv.setOnClickListener {
                WordCard.show(
                    requireContext(), r,
                    { w -> tts?.speak(w, TextToSpeech.QUEUE_FLUSH, null, "cuoci") }
                ) {
                    WordStore.remove(selected.toString(), r.word)
                    render()
                }
            }
            box.addView(tv)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
