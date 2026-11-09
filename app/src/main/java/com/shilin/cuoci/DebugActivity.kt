package com.shilin.cuoci

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** 调试用日志页：用来校准「不背单词」的界面选择器。 */
class DebugActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private var lastShown = ""

    private val refreshTask = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 700L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)

        findViewById<Button>(R.id.btnDump).setOnClickListener {
            val svc = CuociAccessibilityService.instance
            if (svc == null) {
                Toast.makeText(this, "服务还没连上，先去开启无障碍", Toast.LENGTH_SHORT).show()
            } else {
                svc.dumpToLog()
                refresh()
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            LogStore.clear()
            lastShown = ""
            tvLog.text = ""
            refresh()
        }

        findViewById<Button>(R.id.btnCopy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("cuoci-log", LogStore.all().joinToString("\n")))
            Toast.makeText(this, "已复制 " + LogStore.all().size + " 行", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshTask)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshTask)
    }

    private fun refresh() {
        val lines = LogStore.all()
        val tail = if (lines.size > 400) lines.subList(lines.size - 400, lines.size) else lines
        val txt = tail.joinToString("\n")
        if (txt != lastShown) {
            lastShown = txt
            tvLog.text = txt
            scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
