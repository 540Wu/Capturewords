package com.shilin.cuoci

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileFragment : Fragment() {

    private var tvTodayCount: TextView? = null
    private var tvTotalCount: TextView? = null
    private var tvDaysCount: TextView? = null
    private var tvServiceState: TextView? = null
    private var tvUsage: TextView? = null
    private var chart: BarChartView? = null
    private var tab7: TextView? = null
    private var tab15: TextView? = null
    private var tab30: TextView? = null
    private var tabMonth: TextView? = null
    private var range = 7
    private var btnFill: Button? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        UsageStore.init(requireContext())
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvDaysCount = view.findViewById(R.id.tvDaysCount)
        tvServiceState = view.findViewById(R.id.tvServiceState)
        tvUsage = view.findViewById(R.id.tvUsage)
        chart = view.findViewById(R.id.barChart)
        tab7 = view.findViewById(R.id.tab7)
        tab15 = view.findViewById(R.id.tab15)
        tab30 = view.findViewById(R.id.tab30)
        tabMonth = view.findViewById(R.id.tabMonth)

        tab7?.setOnClickListener { range = 7; renderChart() }
        tab15?.setOnClickListener { range = 15; renderChart() }
        tab30?.setOnClickListener { range = 30; renderChart() }
        tabMonth?.setOnClickListener { range = 0; renderChart() }

        btnFill = view.findViewById(R.id.btnFillMeaning)

        btnFill?.setOnClickListener { doFill() }

        view.findViewById<Button>(R.id.btnOpenA11y).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(requireContext(), "找到「错词捕手」并打开开关", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "跳转失败：" + e.message, Toast.LENGTH_LONG).show()
            }
        }

        view.findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnClearToday).setOnClickListener {
            WordStore.clearToday()
            EssayStore.remove(WordStore.todayKey())
            Toast.makeText(requireContext(), "已清空今天的记录", Toast.LENGTH_SHORT).show()
            refresh()
        }

        refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val today = WordStore.today().size
        val days = WordStore.days()
        var total = 0
        for (d in days) total += WordStore.of(d).size

        tvTodayCount?.text = today.toString()
        tvTotalCount?.text = total.toString()
        tvDaysCount?.text = days.size.toString()

        tvUsage?.text = "今日 " + UsageStore.format(UsageStore.todayMs()) +
            " · 累计 " + UsageStore.format(UsageStore.totalMs())

        renderChart()

        val on = isServiceEnabled()
        tvServiceState?.text = if (on) "✅ 无障碍服务：已开启" else "❌ 无障碍服务：未开启"
        tvServiceState?.setTextColor(if (on) 0xFF17864A.toInt() else 0xFFC0392B.toInt())

        val missing = WordStore.missingMeaning().size
        val fill = btnFill ?: return
        if (missing > 0) {
            fill.isEnabled = true
            fill.text = "一键补全缺失的释义（" + missing + " 个）"
        } else {
            fill.isEnabled = false
            fill.text = "释义都已补全"
        }
    }

    /** 用 DeepSeek 批量补全没有释义的词。 */
    private fun doFill() {
        val missing = WordStore.missingMeaning()
        if (missing.isEmpty()) {
            Toast.makeText(requireContext(), "没有缺释义的词了", Toast.LENGTH_SHORT).show()
            return
        }
        val key = Prefs.apiKey(requireContext())
        if (key.isEmpty()) {
            Toast.makeText(requireContext(), "先去设置里填 DeepSeek API Key", Toast.LENGTH_LONG).show()
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            return
        }

        val words = missing.map { it.second.word }.distinct()
        val btn = btnFill ?: return
        btn.isEnabled = false
        btn.text = "补全中…（" + words.size + " 个词）"

        Thread {
            try {
                val items = MeaningFiller.fill(key, words)
                for ((day, rec) in missing) {
                    val hit = items.firstOrNull { it.word.equals(rec.word, ignoreCase = true) }
                    if (hit != null) {
                        WordStore.updateMeaning(day, rec.word, hit.phonetic, hit.meaning)
                    }
                }
                handler.post {
                    if (!isAdded) return@post
                    refresh()
                    Toast.makeText(requireContext(), "已补全 " + items.size + " 个词", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                handler.post {
                    if (!isAdded) return@post
                    refresh()
                    Toast.makeText(requireContext(), "补全失败：" + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** 画柱状图：7/15/30 天按天，0 表示按月。 */
    private fun renderChart() {
        val ctx = context ?: return
        val items = ArrayList<Pair<String, Long>>()

        if (range > 0) {
            val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val labelFmt = SimpleDateFormat("d", Locale.getDefault())
            for (i in range - 1 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -i)
                items.add(Pair(labelFmt.format(c.time), UsageStore.dayMs(dayFmt.format(c.time))))
            }
        } else {
            val keyFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val labelFmt = SimpleDateFormat("M月", Locale.getDefault())
            for (i in 5 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, -i)
                items.add(Pair(labelFmt.format(c.time), UsageStore.monthMs(keyFmt.format(c.time))))
            }
        }

        chart?.setColors(SkinManager.brand(ctx), 0x11000000, SkinManager.textTertiary(ctx))
        chart?.setData(items)

        val on = SkinManager.brand(ctx)
        val off = SkinManager.textTertiary(ctx)
        styleTab(tab7, range == 7, on, off)
        styleTab(tab15, range == 15, on, off)
        styleTab(tab30, range == 30, on, off)
        styleTab(tabMonth, range == 0, on, off)
    }

    private fun styleTab(tv: TextView?, active: Boolean, on: Int, off: Int) {
        tv ?: return
        tv.setTextColor(if (active) on else off)
        tv.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun isServiceEnabled(): Boolean {        val ctx = context ?: return false
        val flat = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.split(':').any { it.contains("CuociAccessibilityService") }
    }
}
