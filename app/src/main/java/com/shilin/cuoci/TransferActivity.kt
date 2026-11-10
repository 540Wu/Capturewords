package com.shilin.cuoci

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 局域网双向同步：一台等，一台连，连上后双方互换数据。
 */
class TransferActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var serving = false

    private lateinit var tvMyIp: TextView
    private lateinit var tvServeState: TextView
    private lateinit var tvFetchState: TextView
    private lateinit var etIp: EditText
    private lateinit var btnServe: Button
    private lateinit var btnFetch: Button
    private lateinit var tvLastIp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinManager.apply(this, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        title = "局域网同步"

        WordStore.init(this)
        EssayStore.init(this)
        UsageStore.init(this)
        PetStore.init(this)

        tvMyIp = findViewById(R.id.tvMyIp)
        tvServeState = findViewById(R.id.tvServeState)
        tvFetchState = findViewById(R.id.tvFetchState)
        etIp = findViewById(R.id.etIp)
        btnServe = findViewById(R.id.btnServe)
        btnFetch = findViewById(R.id.btnFetch)
        tvLastIp = findViewById(R.id.tvLastIp)

        val myIp = LanTransfer.bestIp()
        tvMyIp.text = if (myIp.isEmpty()) "没检测到地址（先连上 WiFi 或热点）" else myIp

        // 自动填入上次同步过的设备
        val last = Prefs.lanIp(this)
        if (last.isNotEmpty()) {
            etIp.setText(last)
            tvLastIp.text = "📌 上次同步的设备：" + last + "（已自动填好）"
            tvLastIp.setOnClickListener {
                etIp.setText(last)
                startSync()
            }
        } else {
            etIp.setText("192.168.43.1")
            tvLastIp.text = ""
        }

        btnServe.setOnClickListener { startServe() }
        btnFetch.setOnClickListener { startSync() }
    }

    private fun startServe() {
        if (serving) return
        serving = true
        btnServe.isEnabled = false
        btnServe.text = "等待中…（点一下取消）"
        btnServe.setOnClickListener { finish() }

        Thread {
            val ok = LanTransfer.serve(
                { Backup.exportJson() },
                120000,
                { s -> handler.post { tvServeState.text = s } },
                { remote ->
                    try {
                        Backup.import(this, remote)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            )
            handler.post {
                serving = false
                btnServe.isEnabled = true
                btnServe.text = "开始同步（等待另一台）"
                btnServe.setOnClickListener { startServe() }
                if (ok) Toast.makeText(this, "双向同步完成", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun startSync() {
        val ip = etIp.text.toString().trim()
        if (ip.isEmpty()) {
            Toast.makeText(this, "先填对方的地址", Toast.LENGTH_SHORT).show()
            return
        }
        if (ip == LanTransfer.bestIp()) {
            Toast.makeText(this, "这是本机地址，请填另一台手机的 IP", Toast.LENGTH_LONG).show()
            return
        }

        btnFetch.isEnabled = false
        btnFetch.text = "同步中…"

        val myJson = Backup.exportJson()

        Thread {
            val remote = LanTransfer.sync(ip, myJson, 60000) { s ->
                handler.post { tvFetchState.text = s }
            }

            if (remote != null) {
                try {
                    val r = Backup.import(this, remote)
                    Prefs.setLanIp(this, ip)
                    handler.post {
                        tvFetchState.text = "✅ 双向同步完成：本机新增 " + r.words + " 个错词、" + r.essays + " 天短文"
                        Toast.makeText(this, "已同步", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    handler.post { tvFetchState.text = "数据解析失败：" + e.message }
                }
            }

            handler.post {
                btnFetch.isEnabled = true
                btnFetch.text = "连接并同步"
            }
        }.start()
    }
}
