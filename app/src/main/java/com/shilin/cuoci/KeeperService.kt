package com.shilin.cuoci

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * 前台常驻服务：挂一条常驻通知，让进程不容易被系统清掉。
 * 抓词、计时、宠物经验都靠它保命。
 */
class KeeperService : Service() {

    companion object {
        private const val CH_ID = "cuoci_keep"
        private const val NOTIF_ID = 1001

        @Volatile
        var running = false
            private set

        fun start(ctx: Context) {
            if (running) return
            val i = Intent(ctx, KeeperService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(i)
                } else {
                    ctx.startService(i)
                }
            } catch (e: Exception) {
                LogStore.add("KEEP", "启动失败：" + e.message)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        try {
            startForeground(NOTIF_ID, buildNotification())
            LogStore.add("KEEP", "常驻服务已启动")
        } catch (e: Exception) {
            LogStore.add("KEEP", "startForeground 失败：" + e.message)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        LogStore.add("KEEP", "常驻服务被销毁")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH_ID, "后台监听", NotificationManager.IMPORTANCE_MIN)
            ch.description = "保持监听「不背单词」，记录错词"
            ch.setShowBadge(false)
            ch.enableVibration(false)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getActivity(this, 0, intent, flags)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CH_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("错词捕手正在监听")
            .setContentText("会自动记录「不背单词」里的错词")
            .setSmallIcon(R.drawable.ic_notify)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
