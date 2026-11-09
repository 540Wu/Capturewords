package com.shilin.cuoci

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 「错词捕手」无障碍服务。
 *
 * 判定规则（基于实测的「不背单词」界面结构）：
 * 1) 选择题（看英文选中文）：答错时会有 2 个选项行处于 selected=true
 *    （你自己点错的那个 + 正确答案那个）；答对只有 1 个。
 * 2) 自评题（认识 / 模糊 / 忘记了）：这三个按钮不上报点击事件，
 *    改成看界面形态：
 *      - 三按钮界面         → 记下当前单词
 *      - 之后出现在同一个单词的「详情页」（底部有 #bt_nextWord 下一词）
 *        → 说明点了「模糊」或「忘记了」→ 记为错词
 *      - 换成了另一个单词   → 说明点了「认识」→ 不记录
 */
class CuociAccessibilityService : AccessibilityService() {

    companion object {
        const val TARGET_PKG = "cn.com.langeasy.LangEasyLexis"

        @Volatile
        var instance: CuociAccessibilityService? = null
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkRunnable = Runnable { checkState() }
    private val dumpRunnable = Runnable { dumpToLog() }
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkState()
            mainHandler.postDelayed(this, 350L)
        }
    }
    private val heartbeat = object : Runnable {
        override fun run() {
            android.util.Log.i("Cuoci", "[HB] 服务存活 事件 " + eventCount + " 条 / 轮询 " + pollCount + " 次")
            eventCount = 0
            pollCount = 0
            checkpointUsage()
            mainHandler.postDelayed(this, 20000L)
        }
    }

    private var eventCount = 0
    private var pollCount = 0
    private var lastSig = ""
    private var detailHit = 0
    private var correctWord = ""
    private var studyStartAt = 0L
    private var lastSeenAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        WordStore.init(this)
        UsageStore.init(this)
        PetStore.init(this)
        LogStore.add("SVC", "无障碍服务已连接")
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.postDelayed(heartbeat, 60000L)
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.postDelayed(pollRunnable, 800L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != TARGET_PKG) return
        eventCount++

        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            scheduleCheck(500)
            return
        }

        val sb = StringBuilder()
        sb.append(typeName(type))
        sb.append(' ').append(shortClass(event.className))
        val txt = event.text?.joinToString(" | ") ?: ""
        if (txt.isNotEmpty()) sb.append("  text=\"").append(cut(txt, 60)).append('"')
        LogStore.add("EVT", sb.toString())

        scheduleCheck(350)
    }

    private fun scheduleCheck(delayMs: Long) {
        mainHandler.removeCallbacks(checkRunnable)
        mainHandler.postDelayed(checkRunnable, delayMs)
    }

    /** 轻量判定：只精确读几个控件，不遍历整棵树。由轮询调用，不依赖事件到达时机。 */
    private fun checkState() {
        if (CuociAccessibilityService.instance !== this) return
        pollCount++

        var root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != TARGET_PKG) {
            var found: AccessibilityNodeInfo? = null
            try {
                for (w in windows) {
                    val r = w.root ?: continue
                    if (r.packageName?.toString() == TARGET_PKG) {
                        found = r
                        break
                    }
                }
            } catch (e: Exception) {
                logSig("windows 枚举失败 " + e.message)
            }
            if (found != null) {
                if (root != null) root.recycle()
                root = found
            }
        }
        if (root == null) {
            tickUsage(null)
            logSig("root=null")
            return
        }
        try {
            val pkg = root.packageName?.toString() ?: "-"
            if (pkg != TARGET_PKG) {
                tickUsage(pkg)
                logSig("前台是 " + pkg)
                return
            }
            tickUsage(TARGET_PKG)
            val rawWord = textOfId(root, "tv_word")
            val word = normWord(rawWord)
            val phonetic = textOfId(root, "prononce_display")
            val meaning = pickMeaning(root)

            // 宠物经验：读「不背单词」顶部的今日进度（如 42/173）
            val learnStatus = textOfId(root, "tv_learn_status")
            val learned = learnStatus.substringBefore('/').trim().toIntOrNull()
            if (learned != null) PetStore.onLearnedCount(learned)
            val hasOptions = nodeById(root, "ll_selectItem_1") != null
            val selCount = countSelectedRows(root)
            val hasNext = nodeById(root, "bt_nextWord") != null
            val hasTick = nodeById(root, "word_finish_tick") != null

            logSig(
                "词=" + word + " 选项=" + (if (hasOptions) "Y" else "N") +
                    " 选中=" + selCount + " 详情=" + (if (hasNext) "Y" else "N") +
                    " 勾=" + (if (hasTick) "Y" else "N")
            )

            // 1) 选项界面（选择题）
            if (hasOptions) {
                detailHit = 0
                if (selCount >= 2 && word.isNotEmpty()) {
                    WordStore.add(word, "选择题·选错", phonetic, meaning)
                    correctWord = ""
                } else if (selCount == 1 && word.isNotEmpty()) {
                    correctWord = word
                }
                return
            }

            // 2) 详情页
            if (hasNext) {
                if (hasTick) {
                    // 有绿勾 = 认定为「认识」
                    correctWord = ""
                    detailHit = 0
                    return
                }
                if (correctWord.isNotEmpty() && word == correctWord) {
                    // 这是答题答对后的释义页，不算错
                    detailHit = 0
                    return
                }
                if (correctWord.isNotEmpty() && word != correctWord) correctWord = ""
                detailHit++
                if (detailHit == 1 && word.isNotEmpty()) {
                    WordStore.add(word, "没记住", phonetic, meaning)
                }
            } else {
                detailHit = 0
            }
        } catch (e: Exception) {
            LogStore.add("ERR", "checkState " + e.message)
        } finally {
            root.recycle()
        }
    }

    /** 只在状态变化时写一行，避免刷屏。 */    /** 对比单词时去掉音节分隔点等符号（complex vs com·plex）。 */
    private fun sameWord(a: String, b: String): Boolean {
        return normWord(a) == normWord(b)
    }

    private fun normWord(s: String): String {
        return s.replace("·", "").replace("-", "").replace(" ", "").trim()
    }

    private fun logSig(sig: String) {
        if (sig == lastSig) return
        lastSig = sig
        LogStore.add("POLL", sig)
    }

    override fun onInterrupt() {
        LogStore.add("SVC", "onInterrupt")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        LogStore.add("SVC", "服务已断开（系统关闭了无障碍开关？）")
        flushUsage()
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.removeCallbacks(heartbeat)
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        flushUsage()
        mainHandler.removeCallbacks(checkRunnable)
        mainHandler.removeCallbacks(dumpRunnable)
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.removeCallbacks(heartbeat)
        if (instance === this) instance = null
        super.onDestroy()
    }

    // ======== 下面是调试用（只在「调试日志」页手动触发，不参与自动判定） ========

    fun dumpToLog() {
        var root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != TARGET_PKG) {
            var found: AccessibilityNodeInfo? = null
            try {
                for (w in windows) {
                    val r = w.root ?: continue
                    if (r.packageName?.toString() == TARGET_PKG) {
                        found = r
                        break
                    }
                }
            } catch (e: Exception) {
                LogStore.add("DUMP", "枚举窗口失败：" + e.message)
            }
            if (found != null) root = found
        }
        if (root == null) {
            LogStore.add("DUMP", "拿不到界面（root = null）")
            return
        }

        val pkg = root.packageName?.toString() ?: "?"
        LogStore.add("DUMP", "===== 快照开始 pkg=" + pkg + " =====")
        logState(root)
        val counter = intArrayOf(0)
        walk(root, 0, counter)
        LogStore.add("DUMP", "===== 快照结束（" + counter[0] + " 个节点）=====")
        root.recycle()
    }

    private fun logState(root: AccessibilityNodeInfo) {
        val word = textOfId(root, "tv_word")
        val status = textOfId(root, "tv_learn_status")
        val accept = textOfId(root, "tv_acceptation")
        val know = nodeById(root, "tv_know")
        val dim = nodeById(root, "tv_dim")
        val unk = nodeById(root, "tv_unknow")
        val next = nodeById(root, "bt_nextWord")

        val sb = StringBuilder()
        sb.append("词=").append(word)
        sb.append(" 进度=").append(status)
        sb.append(" 认识=").append(know != null)
        sb.append(" 模糊=").append(dim != null)
        sb.append(" 忘=").append(unk != null)
        sb.append(" 详情页=").append(next != null)
        sb.append(" 选中行=").append(countSelectedRows(root))
        if (accept.isNotEmpty()) sb.append(" 释义=").append(cut(accept, 30))
        LogStore.add("STATE", sb.toString())
    }

    /** 前台包名变化时累计学习时长。 */
    private fun tickUsage(frontPkg: String?) {
        val now = System.currentTimeMillis()
        if (frontPkg == TARGET_PKG) {
            if (studyStartAt == 0L) studyStartAt = now
            lastSeenAt = now
        } else if (frontPkg != null && studyStartAt > 0L) {
            // 离开目标应用，超过 3 秒没回来就算一段
            if (now - lastSeenAt > 3000) flushUsage()
        }
    }

    /** 结束当前一段计时并写入。 */
    private fun flushUsage() {
        if (studyStartAt <= 0L) return
        val dur = lastSeenAt - studyStartAt
        if (dur > 3000) UsageStore.add(dur)
        studyStartAt = 0L
        lastSeenAt = 0L
    }

    /** 定时把已累计的部分落盘，避免进程被杀时丢太多。 */
    private fun checkpointUsage() {
        if (studyStartAt <= 0L) return
        val dur = lastSeenAt - studyStartAt
        if (dur > 3000) {
            UsageStore.add(dur)
            studyStartAt = lastSeenAt
        }
    }

    /** 优先取中文释义，没有就用英文释义。 */
    private fun pickMeaning(root: AccessibilityNodeInfo): String {
        val cn = textOfId(root, "tv_interpretItem_meaning")
        if (cn.isNotEmpty()) return cn
        val en = textOfId(root, "tv_acceptation")
        if (en.isNotEmpty()) return en
        return ""
    }

    private fun countSelectedRows(root: AccessibilityNodeInfo): Int {
        var n = 0
        for (id in arrayOf("tv_selectItem", "tv_word_meaning")) {
            val list = try {
                root.findAccessibilityNodeInfosByViewId(TARGET_PKG + ":id/" + id)
            } catch (e: Exception) {
                null
            }
            if (list != null) {
                for (node in list) {
                    if (node.isSelected) n++
                }
            }
        }
        return n
    }

    private fun nodeById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        return try {
            val list = root.findAccessibilityNodeInfosByViewId(TARGET_PKG + ":id/" + id)
            if (list.isNullOrEmpty()) null else list[0]
        } catch (e: Exception) {
            null
        }
    }

    private fun textOfId(root: AccessibilityNodeInfo, id: String): String {
        val n = nodeById(root, id) ?: return ""
        return n.text?.toString()?.trim() ?: ""
    }

    private fun walk(n: AccessibilityNodeInfo, depth: Int, counter: IntArray) {
        if (depth > 24 || counter[0] > 200) return
        counter[0]++

        val t = n.text?.toString()?.trim() ?: ""
        val d = n.contentDescription?.toString()?.trim() ?: ""
        if (t.isNotEmpty() || d.isNotEmpty() || n.isClickable) {
            val sb = StringBuilder()
            for (i in 0 until depth) sb.append("  ")
            sb.append(shortClass(n.className))
            val id = n.viewIdResourceName
            if (!id.isNullOrEmpty()) sb.append(" #").append(id.substringAfterLast('/'))
            if (t.isNotEmpty()) sb.append(" t=\"").append(cut(t, 40)).append('"')
            if (d.isNotEmpty()) sb.append(" d=\"").append(cut(d, 40)).append('"')
            val r = Rect()
            n.getBoundsInScreen(r)
            sb.append(" @").append(r.top)
            if (n.isClickable) sb.append(" [可点]")
            if (n.isSelected) sb.append(" [已选]")
            LogStore.add("TREE", sb.toString())
        }

        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            walk(c, depth + 1, counter)
            c.recycle()
        }
    }

    private fun shortClass(cs: CharSequence?): String {
        val s = cs?.toString() ?: return "-"
        return s.substringAfterLast('.')
    }

    private fun cut(s: String, max: Int): String {
        val one = s.replace("\n", " ")
        return if (one.length <= max) one else one.substring(0, max) + "…"
    }

    private fun typeName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "界面切换"
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "点击"
        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "长按"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "文字输入"
        AccessibilityEvent.TYPE_VIEW_FOCUSED -> "获得焦点"
        AccessibilityEvent.TYPE_VIEW_SELECTED -> "选中"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "滚动"
        else -> "type=" + type
    }
}
