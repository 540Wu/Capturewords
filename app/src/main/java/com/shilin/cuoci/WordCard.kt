package com.shilin.cuoci

import android.content.Context
import androidx.appcompat.app.AlertDialog

object WordCard {

    /** 词卡弹窗：单词 + 音标 + 释义 + 发音（可选删除）。 */
    fun show(
        ctx: Context,
        rec: WordStore.Rec,
        onSpeak: (String) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        val sb = StringBuilder()
        if (rec.phonetic.isNotEmpty()) {
            sb.append(rec.phonetic).append("\n\n")
        }
        if (rec.meaning.isNotEmpty()) {
            sb.append(rec.meaning)
        } else {
            sb.append("（没抓到释义，可在「我的」里一键补全）")
        }
        sb.append("\n\n记录：").append(rec.reason)
            .append(" · ").append(WordStore.timeText(rec.time))

        val builder = AlertDialog.Builder(ctx)
            .setTitle(rec.word)
            .setMessage(sb.toString())
            .setPositiveButton("发音") { _, _ -> onSpeak(rec.word) }
            .setNegativeButton("关闭", null)

        if (onDelete != null) {
            builder.setNeutralButton("删除") { _, _ ->
                AlertDialog.Builder(ctx)
                    .setTitle("删除「" + rec.word + "」？")
                    .setMessage("会从这一天的记录里移除，不影响你在不背单词里的进度。")
                    .setPositiveButton("删除") { _, _ -> onDelete() }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        builder.show()
    }
}
