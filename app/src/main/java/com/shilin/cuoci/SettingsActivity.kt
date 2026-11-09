package com.shilin.cuoci

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = "设置"

        val etKey = findViewById<EditText>(R.id.etApiKey)
        val tvState = findViewById<TextView>(R.id.tvServiceState)

        PetStore.init(this)
        PetMemory.init(this)

        val etPetName = findViewById<EditText>(R.id.etPetName)
        etPetName.setText(PetStore.petName())

        findViewById<Button>(R.id.btnSavePetName).setOnClickListener {
            PetStore.setPetName(etPetName.text.toString())
            Toast.makeText(this, "名字已保存", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnResetPet).setOnClickListener { confirmResetPet() }

        etKey.setText(Prefs.apiKey(this))

        tvState.text = if (isServiceEnabled()) "✅ 无障碍服务：已开启" else "❌ 无障碍服务：未开启"
        tvState.setTextColor(if (isServiceEnabled()) 0xFF1B7F28.toInt() else 0xFFB3261E.toInt())

        findViewById<Button>(R.id.btnOpenA11y).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "找到「错词捕手」并打开开关", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "跳转失败：" + e.message, Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnDebug).setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val k = etKey.text.toString().trim()
            if (k.isEmpty()) {
                Toast.makeText(this, "Key 不能为空", Toast.LENGTH_SHORT).show()
            } else if (!k.startsWith("sk-")) {
                Toast.makeText(this, "DeepSeek 的 Key 一般以 sk- 开头，确定要保存吗？", Toast.LENGTH_LONG).show()
                Prefs.setApiKey(this, k)
            } else {
                Prefs.setApiKey(this, k)
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun confirmResetPet() {
        AlertDialog.Builder(this)
            .setTitle("重置宠物")
            .setMessage("会清空它的等级、经验和聊天记忆，重新变回一颗蛋。")
            .setPositiveButton("继续") { _, _ -> confirmResetPetFinal() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmResetPetFinal() {
        AlertDialog.Builder(this)
            .setTitle("最后确认")
            .setMessage("重置之后无法恢复，真的要这样做吗？")
            .setPositiveButton("确认重置") { _, _ ->
                PetStore.reset()
                PetMemory.clear()
                findViewById<EditText>(R.id.etPetName).setText("")
                Toast.makeText(this, "宠物已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("算了", null)
            .show()
    }

    private fun isServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.split(':').any { it.contains("CuociAccessibilityService") }
    }
}
