package com.shilin.cuoci

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PetActivity : AppCompatActivity() {

    private val history = ArrayList<Pair<Boolean, String>>()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollChat: ScrollView
    private lateinit var etMsg: EditText
    private lateinit var btnSend: Button
    private lateinit var tvEmoji: TextView
    private lateinit var tvPetName: TextView
    private lateinit var tvPetLevel: TextView
    private lateinit var tvPetHint: TextView
    private lateinit var pbExp: ProgressBar

    private var breath: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet)
        title = "我的宠物"
        PetStore.init(this)
        WordStore.init(this)
        PetMemory.init(this)

        chatContainer = findViewById(R.id.chatContainer)
        scrollChat = findViewById(R.id.scrollChat)
        etMsg = findViewById(R.id.etMsg)
        btnSend = findViewById(R.id.btnSend)
        tvEmoji = findViewById(R.id.tvEmoji)
        tvPetName = findViewById(R.id.tvPetName)
        tvPetLevel = findViewById(R.id.tvPetLevel)
        tvPetHint = findViewById(R.id.tvPetHint)
        pbExp = findViewById(R.id.pbExp)

        // 点名字可以改名
        tvPetName.setOnClickListener { renameDialog() }
        tvEmoji.setOnClickListener { bounce() }

        btnSend.setOnClickListener { send() }

        refreshPet()
        startBreath()
        loadHistory()
    }

    override fun onDestroy() {
        breath?.cancel()
        breath = null
        super.onDestroy()
    }

    private fun refreshPet() {
        tvEmoji.text = PetStore.emoji()
        tvPetName.text = PetStore.displayName() + " · Lv." + PetStore.level()
        tvPetLevel.text = "经验 " + PetStore.progressInLevel() + " / " + PetStore.progressNeed() +
            " · 今天 +" + PetStore.todayGain()
        pbExp.max = PetStore.progressNeed()
        pbExp.progress = PetStore.progressInLevel()
        tvPetHint.text = PetStore.nextFormHint()
    }

    private fun renameDialog() {
        val input = EditText(this)
        input.setText(PetStore.petName())
        input.hint = "给它起个名字"
        input.setPadding(dp(20), dp(12), dp(20), dp(12))
        input.textSize = 16f

        AlertDialog.Builder(this)
            .setTitle("给它起个名字")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                PetStore.setPetName(input.text.toString())
                refreshPet()
                bounce()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadHistory() {
        history.clear()
        history.addAll(PetMemory.load())
        if (history.isEmpty()) {
            greet()
        } else {
            for ((isUser, text) in history) addBubble(isUser, text)
        }
        maybeShowTip()
    }

    /** 每天主动提醒一次：高频词 / 熟词僻义 / 好词好句 */
    private fun maybeShowTip() {
        val today = WordStore.todayKey()
        if (Prefs.tipDay(this) == today) return
        Prefs.setTipDay(this, today)
        val tip = PetTips.todayTip()
        addBubble(false, tip)
        history.add(Pair(false, tip))
        PetMemory.save(history)
    }

    private fun greet() {
        val level = PetStore.level()
        val text = when {
            PetStore.exp() == 0 -> "我还没出生呢…先去背几个单词好不好 🥚"
            level <= 4 -> "（壳里传来闷闷的声音）…外面是你吗？再背一点我就听得更清楚了 🥚"
            level <= 9 -> "啪嗒一声我出来了！你、你好呀……这里是哪里？🐣"
            level <= 19 -> "今天也要一起背单词呀！我陪你～🐥"
            level <= 34 -> "我认识好多字了哦！不信你考考我…先背十个单词好不好 🐤"
            level <= 54 -> "打起精神来！今天的目标背完了吗？我盯着你呢 🐔"
            level <= 79 -> "别急，一天一天来。等你站到高处，就知道坚持有多值 🦅"
            else -> "……来了？把单词背了，我护着你。🐲"
        }
        addBubble(false, text)
        history.add(Pair(false, text))
        PetMemory.save(history)
    }

    private fun send() {
        val text = etMsg.text.toString().trim()
        if (text.isEmpty()) return

        val key = Prefs.apiKey(this)
        if (key.isEmpty()) {
            Toast.makeText(this, "先去设置里填 DeepSeek API Key", Toast.LENGTH_LONG).show()
            return
        }

        // 每天最多 60 条，防止一直聊烧 token
        if (Prefs.petChatCount(this) >= 60) {
            Toast.makeText(this, "今天聊得够多啦，明天再来～", Toast.LENGTH_LONG).show()
            return
        }
        Prefs.incPetChat(this)

        etMsg.setText("")
        addBubble(true, text)
        history.add(Pair(true, text))
        PetMemory.save(history)

        btnSend.isEnabled = false
        btnSend.text = "…"

        val snapshot = ArrayList(history)
        val todayWords = PetStore.todayGain()

        Thread {
            var reply: String
            try {
                reply = PetChat.reply(
                    key,
                    PetStore.displayName(),
                    PetStore.level(),
                    todayWords,
                    snapshot,
                    text
                )
            } catch (e: Exception) {
                reply = "呜…我脑子卡住了（" + (e.message ?: "网络问题") + "）"
            }
            handler.post {
                btnSend.isEnabled = true
                btnSend.text = "发送"
                addBubble(false, reply)
                history.add(Pair(false, reply))
                PetMemory.save(history)
                bounce()
            }
        }.start()
    }

    private fun addBubble(isUser: Boolean, text: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = if (isUser) Gravity.END else Gravity.START
        row.setPadding(0, 6, 0, 6)

        val bubble = TextView(this)
        bubble.text = text
        bubble.textSize = 15f
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10))

        val big = dp(16).toFloat()
        val small = dp(4).toFloat()
        val bg = GradientDrawable()
        if (isUser) {
            bg.setColor(SkinManager.brand(this))
            bg.cornerRadii = floatArrayOf(big, big, big, big, small, small, big, big)
            bubble.setTextColor(SkinManager.colorOnBrand(this))
        } else {
            bg.setColor(SkinManager.cardSolid(this))
            bg.cornerRadii = floatArrayOf(big, big, big, big, big, big, small, small)
            bubble.setTextColor(SkinManager.textMain(this))
        }
        bubble.background = bg
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.leftMargin = dp(40)
        lp.rightMargin = dp(40)
        bubble.layoutParams = lp

        row.addView(bubble)
        chatContainer.addView(row)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    /** 呼吸 + 轻轻浮动 */
    private fun startBreath() {
        breath?.cancel()
        breath = ObjectAnimator.ofPropertyValuesHolder(
            tvEmoji,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.07f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.07f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -dp(5).toFloat(), 0f)
        )
        breath?.duration = 2000L
        breath?.repeatCount = ObjectAnimator.INFINITE
        breath?.interpolator = AccelerateDecelerateInterpolator()
        breath?.start()
    }

    /** 被点/说话时跳一下 */
    private fun bounce() {
        breath?.cancel()
        tvEmoji.animate().scaleX(1.3f).scaleY(1.3f).setDuration(110).withEndAction {
            tvEmoji.animate().scaleX(1f).scaleY(1f).setDuration(160).withEndAction {
                startBreath()
            }.start()
        }.start()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
