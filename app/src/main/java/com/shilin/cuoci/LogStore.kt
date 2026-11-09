package com.shilin.cuoci

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 内存里的日志缓冲。无障碍服务和界面在同一个进程，所以两边能共用这个单例。
 */
object LogStore {

    private const val MAX = 2000
    private val buf = ArrayList<String>(MAX)
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private var lastMsg = ""
    private var lastAt = 0L

    @Synchronized
    fun add(tag: String, msg: String) {
        val now = System.currentTimeMillis()
        // 去重：300ms 内完全相同的一行就丢弃，防止内容变化事件刷屏
        if (msg == lastMsg && now - lastAt < 300) return
        lastMsg = msg
        lastAt = now
        buf.add(fmt.format(Date(now)) + " [" + tag + "] " + msg)
        while (buf.size > MAX) buf.removeAt(0)
        Log.i("Cuoci", "[" + tag + "] " + msg)
    }

    @Synchronized
    fun all(): List<String> = ArrayList(buf)

    @Synchronized
    fun clear() {
        buf.clear()
        lastMsg = ""
        lastAt = 0L
    }
}
