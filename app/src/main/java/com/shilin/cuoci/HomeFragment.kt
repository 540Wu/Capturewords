package com.shilin.cuoci

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.util.Locale

class HomeFragment : Fragment() {

    private var tvStatus: TextView? = null
    private var tvToday: TextView? = null
    private var container: LinearLayout? = null
    private var btnGenerate: Button? = null
    private var tvPetEmoji: TextView? = null
    private var tvPetName: TextView? = null
    private var tvPetInfo: TextView? = null
    private var tvPetTip: TextView? = null

    private var btnSelect: TextView? = null
    private var selectBar: LinearLayout? = null
    private var tvSelectedCount: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var lastKey = ""
    private var tts: TextToSpeech? = null
    private var running = false

    private var selectMode = false
    private val selectedWords = HashSet<String>()

    private val refreshTask = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvStatus = view.findViewById(R.id.tvStatus)
        tvToday = view.findViewById(R.id.tvToday)
        container = view.findViewById(R.id.listContainer)
        btnGenerate = view.findViewById(R.id.btnGenerate)
        tvPetEmoji = view.findViewById(R.id.tvPetEmoji)
        tvPetName = view.findViewById(R.id.tvPetName)
        tvPetInfo = view.findViewById(R.id.tvPetInfo)
        tvPetTip = view.findViewById(R.id.tvPetTip)
        btnSelect = view.findViewById(R.id.btnSelect)
        selectBar = view.findViewById(R.id.selectBar)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)

        PetStore.init(requireContext())
        view.findViewById<LinearLayout>(R.id.petCard).setOnClickListener {
            startActivity(Intent(requireContext(), PetActivity::class.java))
        }

        btnGenerate?.setOnClickListener { startGenerate() }
        view.findViewById<Button>(R.id.btnConfuse).setOnClickListener {
            if (WordStore.today().isEmpty()) {
                Toast.makeText(requireContext(), "今天还没有错词", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(requireContext(), ConfuseActivity::class.java))
            }
        }

        btnSelect?.setOnClickListener { setSelectMode(true) }
        view.findViewById<TextView>(R.id.btnAdd).setOnClickListener { showAddDialog() }
        view.findViewById<TextView>(R.id.btnCancelSelect).setOnClickListener { setSelectMode(false) }
        view.findViewById<TextView>(R.id.btnSelectAll).setOnClickListener { selectAll() }
        view.findViewById<TextView>(R.id.btnDeleteSelected).setOnClickListener { deleteSelected() }

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) startRefresh()
    }

    override fun onPause() {
        super.onPause()
        stopRefresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) stopRefresh() else {
            startRefresh()
            lastKey = ""
            refresh()
        }
    }

    override fun onDestroyView() {
        stopRefresh()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroyView()
    }

    private fun startRefresh() {
        if (running) return
        running = true
        handler.post(refreshTask)
    }

    private fun stopRefresh() {
        running = false
        handler.removeCallbacks(refreshTask)
    }

    // ===== 多选 =====

    private fun setSelectMode(on: Boolean) {
        selectMode = on
        if (!on) selectedWords.clear()
        selectBar?.visibility = if (on) View.VISIBLE else View.GONE
        btnSelect?.visibility = if (on) View.GONE else View.VISIBLE
        lastKey = ""
        refresh()
    }

    private fun selectAll() {
        val all = WordStore.today().map { it.word }
        if (selectedWords.size == all.size && all.isNotEmpty()) {
            selectedWords.clear()
        } else {
            selectedWords.clear()
            selectedWords.addAll(all)
        }
        lastKey = ""
        refresh()
    }

    private fun toggleWord(word: String) {
        if (selectedWords.contains(word)) selectedWords.remove(word) else selectedWords.add(word)
        lastKey = ""
        refresh()
    }

    private fun deleteSelected() {
        if (selectedWords.isEmpty()) {
            Toast.makeText(requireContext(), "还没选中任何词", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("删除 " + selectedWords.size + " 个错词？")
            .setMessage("只删掉本 App 的记录，不影响你在「不背单词」里的进度。")
            .setPositiveButton("删除") { _, _ ->
                val day = WordStore.todayKey()
                for (w in selectedWords) WordStore.remove(day, w)
                selectedWords.clear()
                setSelectMode(false)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddDialog() {
        val input = EditText(requireContext())
        input.hint = "比如：aggravate，多个用空格或逗号分隔"
        input.textSize = 15f
        input.setPadding(dp(20), dp(12), dp(20), dp(12))

        AlertDialog.Builder(requireContext())
            .setTitle("手动添加错词")
            .setView(input)
            .setPositiveButton("添加") { _, _ ->
                val raw = input.text.toString()
                val words = raw.split(Regex("[,，\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
                if (words.isEmpty()) return@setPositiveButton
                for (w in words) WordStore.add(w, "手动添加")
                lastKey = ""
                refresh()
                Toast.makeText(requireContext(), "已添加 " + words.size + " 个词", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ===== 列表 =====

    private fun refresh() {
        val status = tvStatus ?: return
        if (isServiceEnabled()) {
            status.text = "✅ 正在监听「不背单词」"
            status.setTextColor(0xFF17864A.toInt())
        } else {
            status.text = "❌ 无障碍服务未开启，去「我的」里打开"
            status.setTextColor(0xFFC0392B.toInt())
        }

        val list = WordStore.today()
        tvToday?.text = "今天 " + DayUtil.shortLabel(WordStore.todayKey()) + " · " + list.size + " 个错词"

        tvPetEmoji?.text = PetStore.emoji()
        tvPetName?.text = PetStore.displayName() + " · Lv." + PetStore.level()
        tvPetInfo?.text = "经验 " + PetStore.progressInLevel() + " / " + PetStore.progressNeed() +
            " · 今天 +" + PetStore.todayGain()
        tvPetTip?.text = "💭 " + PetTips.todayTip()

        tvSelectedCount?.text = "已选 " + selectedWords.size + " 个"

        val key = "L:" + selectMode + ":" + selectedWords.joinToString(",") + ":" +
            list.joinToString("|") { it.word + it.reason + it.time }
        if (key == lastKey) return
        lastKey = key
        renderList(list)
    }

    private fun renderList(list: List<WordStore.Rec>) {
        val box = container ?: return
        box.removeAllViews()
        if (list.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = "还没有记录。\n\n去「不背单词」背几个词，\n答错或点「不认识 / 模糊」的词\n会自动出现在这里。\n\n（也可以用右上角「+ 添加」手动加）"
            tv.setTextColor(SkinManager.textSub(requireContext()))
            tv.textSize = 14f
            tv.setPadding(8, 24, 8, 24)
            box.addView(tv)
            return
        }

        for ((index, r) in list.withIndex()) {
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(8, 16, 8, 16)
            row.gravity = Gravity.CENTER_VERTICAL

            if (selectMode) {
                val cb = TextView(requireContext())
                cb.text = if (selectedWords.contains(r.word)) "☑" else "☐"
                cb.textSize = 20f
                cb.setTextColor(SkinManager.brand(requireContext()))
                cb.setPadding(0, 0, dp(10), 0)
                row.addView(cb)
            }

            val left = LinearLayout(requireContext())
            left.orientation = LinearLayout.VERTICAL
            left.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val tw = TextView(requireContext())
            tw.text = if (r.phonetic.isEmpty()) r.word else r.word + "  " + r.phonetic
            tw.textSize = 18f
            tw.setTextColor(SkinManager.textMain(requireContext()))
            tw.setTypeface(null, android.graphics.Typeface.BOLD)
            left.addView(tw)

            if (r.meaning.isNotEmpty()) {
                val tm = TextView(requireContext())
                tm.text = r.meaning
                tm.textSize = 12f
                tm.setTextColor(SkinManager.textSub(requireContext()))
                tm.maxLines = 1
                tm.ellipsize = android.text.TextUtils.TruncateAt.END
                left.addView(tm)
            }

            val ts = TextView(requireContext())
            ts.text = r.reason + " · " + WordStore.timeText(r.time)
            ts.textSize = 11f
            ts.setTextColor(SkinManager.textTertiary(requireContext()))
            left.addView(ts)

            row.addView(left)

            row.setOnClickListener {
                if (selectMode) {
                    toggleWord(r.word)
                } else {
                    WordCard.show(
                        requireContext(), r,
                        { w -> tts?.speak(w, TextToSpeech.QUEUE_FLUSH, null, "cuoci") }
                    ) {
                        WordStore.remove(WordStore.todayKey(), r.word)
                        lastKey = ""
                        refresh()
                    }
                }
            }

            row.setOnLongClickListener {
                if (!selectMode) {
                    setSelectMode(true)
                    selectedWords.add(r.word)
                    lastKey = ""
                    refresh()
                }
                true
            }

            box.addView(row)

            // 逐条滑入
            if (index < 12) {
                row.alpha = 0f
                row.translationY = dp(18).toFloat()
                row.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(index * 28L)
                    .setDuration(240L)
                    .start()
            }

            val divider = View(requireContext())
            divider.setBackgroundColor(SkinManager.line(requireContext()))
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            box.addView(divider)
        }
    }

    // ===== 生成短文 =====

    private fun startGenerate() {
        val words = WordStore.today().map { it.word }
        if (words.isEmpty()) {
            Toast.makeText(requireContext(), "今天还没有错词，先去背几个吧", Toast.LENGTH_SHORT).show()
            return
        }
        val key = Prefs.apiKey(requireContext())
        if (key.isEmpty()) {
            Toast.makeText(requireContext(), "还没有填 DeepSeek API Key，先去设置里填一下", Toast.LENGTH_LONG).show()
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            return
        }

        val day = WordStore.todayKey()
        val cached = EssayStore.get(day)
        if (cached != null && cached.parts.isNotEmpty()) {
            askCached(cached, words, key, day)
        } else {
            doGenerate(words, key, day)
        }
    }

    private fun askCached(entry: EssayStore.Entry, words: List<String>, key: String, day: String) {
        val added = words.filter { w -> entry.words.none { it.equals(w, ignoreCase = true) } }
        val msg: String = if (added.isEmpty()) {
            "今天已经生成过短文（" + entry.parts.size + " 组）。\n要看之前的，还是重新生成一篇新的？"
        } else {
            "今天又新抓到 " + added.size + " 个错词：" +
                added.take(6).joinToString(", ") +
                (if (added.size > 6) " 等" else "") +
                "\n\n要看之前的短文，还是用全部 " + words.size + " 个错词重新生成？"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("今日短文")
            .setMessage(msg)
            .setPositiveButton("看之前的") { _, _ ->
                ResultActivity.parts = entry.parts
                startActivity(Intent(requireContext(), ResultActivity::class.java))
            }
            .setNegativeButton("重新生成") { _, _ -> doGenerate(words, key, day) }
            .setNeutralButton("取消", null)
            .show()
    }

    private fun doGenerate(words: List<String>, key: String, day: String) {
        val btn = btnGenerate ?: return
        btn.isEnabled = false
        val groups = (words.size + EssayGenerator.GROUP_SIZE - 1) / EssayGenerator.GROUP_SIZE
        btn.text = "生成中…（共 " + groups + " 组）"

        Thread {
            try {
                val parts = EssayGenerator.generateAll(key, words) { done, total ->
                    handler.post { btn.text = "生成中… " + done + "/" + total + " 组" }
                }
                EssayStore.save(day, words, parts)
                handler.post {
                    if (!isAdded) return@post
                    btn.isEnabled = true
                    btn.text = "用这些错词生成短文"
                    ResultActivity.parts = parts
                    startActivity(Intent(requireContext(), ResultActivity::class.java))
                }
            } catch (e: Exception) {
                handler.post {
                    if (!isAdded) return@post
                    btn.isEnabled = true
                    btn.text = "用这些错词生成短文"
                    Toast.makeText(requireContext(), "生成失败：" + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun isServiceEnabled(): Boolean {
        val ctx = context ?: return false
        val flat = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.split(':').any { it.contains("CuociAccessibilityService") }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
