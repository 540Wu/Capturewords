package com.shilin.cuoci

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.util.Locale

/**
 * 复习自测：看单词回忆意思，再自评「记住了 / 没记住」。
 */
class QuizFragment : Fragment() {

    private var queue: List<WordStore.Rec> = emptyList()
    private var index = 0
    private var known = 0
    private val forgotList = ArrayList<WordStore.Rec>()

    private var tvWord: TextView? = null
    private var tvPhonetic: TextView? = null
    private var tvMeaning: TextView? = null
    private var tvHint: TextView? = null
    private var tvProgress: TextView? = null
    private var btnShow: Button? = null
    private var btnSpeak: Button? = null
    private var judgeRow: LinearLayout? = null

    private var tts: TextToSpeech? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_quiz, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvWord = view.findViewById(R.id.tvWord)
        tvPhonetic = view.findViewById(R.id.tvPhonetic)
        tvMeaning = view.findViewById(R.id.tvMeaning)
        tvHint = view.findViewById(R.id.tvHint)
        tvProgress = view.findViewById(R.id.tvProgress)
        btnShow = view.findViewById(R.id.btnShow)
        btnSpeak = view.findViewById(R.id.btnSpeak)
        judgeRow = view.findViewById(R.id.judgeRow)

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        btnShow?.setOnClickListener { revealMeaning() }

        btnSpeak?.setOnClickListener {
            val w = queue.getOrNull(index)?.word ?: return@setOnClickListener
            tts?.speak(w, TextToSpeech.QUEUE_FLUSH, null, "cuoci")
        }

        view.findViewById<Button>(R.id.btnKnow).setOnClickListener {
            known++
            index++
            showCurrent()
        }

        view.findViewById<Button>(R.id.btnForgot).setOnClickListener {
            queue.getOrNull(index)?.let { forgotList.add(it) }
            index++
            showCurrent()
        }

        load()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && index >= queue.size) load()
    }

    override fun onDestroyView() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroyView()
    }

    private fun load() {
        val today = WordStore.today()
        queue = if (today.isNotEmpty()) {
            today
        } else {
            val days = WordStore.days()
            if (days.isEmpty()) emptyList() else WordStore.of(days[0])
        }
        index = 0
        known = 0
        forgotList.clear()

        if (queue.isEmpty()) {
            tvWord?.text = "还没有可复习的词"
            tvPhonetic?.text = ""
            tvMeaning?.visibility = View.GONE
            tvHint?.text = "先去「不背单词」背几个词吧"
            judgeRow?.visibility = View.GONE
            btnShow?.visibility = View.GONE
            btnSpeak?.visibility = View.GONE
            tvProgress?.text = ""
            return
        }

        btnShow?.visibility = View.VISIBLE
        btnSpeak?.visibility = View.VISIBLE
        showCurrent()
    }

    private fun showCurrent() {
        if (index >= queue.size) {
            finishQuiz()
            return
        }
        val r = queue[index]
        tvWord?.text = r.word
        tvPhonetic?.text = r.phonetic
        tvMeaning?.text = if (r.meaning.isEmpty()) "（这个词没抓到释义）" else r.meaning
        tvMeaning?.visibility = View.GONE
        tvHint?.visibility = View.VISIBLE
        tvHint?.text = "先回忆意思，再点下面的按钮"
        judgeRow?.visibility = View.GONE
        btnShow?.visibility = View.VISIBLE
        tvProgress?.text = (index + 1).toString() + " / " + queue.size
    }

    private fun revealMeaning() {
        tvMeaning?.visibility = View.VISIBLE
        tvHint?.visibility = View.GONE
        judgeRow?.visibility = View.VISIBLE
        btnShow?.visibility = View.GONE
    }

    private fun finishQuiz() {
        val total = queue.size
        val forgotCount = forgotList.size
        val msg = "这轮共 " + total + " 个词\n\n记住了 " + known + " 个，没记住 " + forgotCount + " 个"

        if (!isAdded) return

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("复习完成")
            .setMessage(msg)

        if (forgotCount > 0) {
            builder.setPositiveButton("再复习没记住的（" + forgotCount + "）") { _, _ ->
                queue = ArrayList(forgotList)
                index = 0
                known = 0
                forgotList.clear()
                showCurrent()
            }
        } else {
            builder.setPositiveButton("很好，全记住了") { _, _ ->
                index = 0
                showCurrent()
            }
        }

        builder.setNegativeButton("结束", null)
        builder.show()

        tvHint?.text = "这一轮结束了，可以再来一遍"
        tvHint?.visibility = View.VISIBLE
        tvMeaning?.visibility = View.GONE
        judgeRow?.visibility = View.GONE
        btnShow?.visibility = View.GONE
        tvProgress?.text = total.toString() + " / " + total
    }
}
