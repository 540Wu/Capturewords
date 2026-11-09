package com.shilin.cuoci

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 新手指引：带状态检测的步骤清单，每步都能直接跳过去设置。
 */
class GuideActivity : AppCompatActivity() {

    private lateinit var tvStateKey: TextView
    private lateinit var tvStateA11y: TextView
    private lateinit var tvStateKeep: TextView
    private lateinit var tvHeadline: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        title = "新手指引"
        PetStore.init(this)

        tvStateKey = findViewById(R.id.tvStateKey)
        tvStateA11y = findViewById(R.id.tvStateA11y)
        tvStateKeep = findViewById(R.id.tvStateKeep)
        tvHeadline = findViewById(R.id.tvHeadline)

        findViewById<Button>(R.id.btnKey).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnA11y).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "找到「错词捕手」并打开开关", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "跳转失败：" + e.message, Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnKeep).setOnClickListener {
            try {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:" + packageName)
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(this, "打不开应用设置：" + e.message, Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val hasKey = Prefs.apiKey(this).isNotEmpty()
        val hasA11y = isServiceEnabled()
        val hasKeep = KeeperService.running

        setState(tvStateKey, hasKey)
        setState(tvStateA11y, hasA11y)
        setState(tvStateKeep, hasKeep)

        val done = listOf(hasKey, hasA11y, hasKeep).count { it }
        tvHeadline.text = if (done == 3) {
            "✅ 全部设置好了，打开「不背单词」正常学习就行"
        } else {
            "跟着做完下面 " + (3 - done) + " 步，它就能自动帮你记错词了"
        }
    }

    private fun setState(tv: TextView, ok: Boolean) {
        if (ok) {
            tv.text = "✅ 已完成"
            tv.setTextColor(SkinManager.color(this, R.attr.textMain))
        } else {
            tv.text = "未完成"
            tv.setTextColor(SkinManager.color(this, R.attr.textSub))
        }
    }

    private fun isServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.split(':').any { it.contains("CuociAccessibilityService") }
    }
}
