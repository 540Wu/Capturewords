package com.shilin.cuoci

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object WordCard {

    /**
     * 词卡：单词 + 音标 + 释义 + 词形变化 + 发音 / 近义词 / 删除。
     */
    fun show(
        ctx: Context,
        rec: WordStore.Rec,
        onSpeak: (String) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        // 本地词库兜底
        val bank = WordBank.lookup(rec.word)
        val phonetic = if (rec.phonetic.isNotEmpty()) rec.phonetic else (bank?.phonetic ?: "")
        val meaning = if (rec.meaning.isNotEmpty()) rec.meaning else (bank?.meaning ?: "")

        val sb = StringBuilder()
        if (phonetic.isNotEmpty()) sb.append(phonetic).append("\n")

        sb.append(if (meaning.isNotEmpty()) meaning else "（本地词库没有这个词，可在「我的」里一键补全）")

        // 词形变化（本地计算，不花 token）
        val a = WordForm.analyze(rec.word)
        if (a.base.isNotEmpty()) {
            sb.append("\n\n原型：").append(a.base)
        }
        if (a.forms.isNotEmpty()) {
            sb.append("\n词形：")
            sb.append(a.forms.joinToString("   ") { it.first + " " + it.second })
        }

        sb.append("\n\n记录：").append(rec.reason)
            .append(" · ").append(WordStore.timeText(rec.time))

        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_word, null)
        view.findViewById<TextView>(R.id.tvWordTitle).text = rec.word
        view.findViewById<TextView>(R.id.tvWordBody).text = sb.toString()

        val dialog = AlertDialog.Builder(ctx).setView(view).create()

        view.findViewById<Button>(R.id.btnSpeak).setOnClickListener { onSpeak(rec.word) }
        view.findViewById<Button>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnSyn).setOnClickListener {
            dialog.dismiss()
            showSynonyms(ctx, rec.word)
        }

        val del = view.findViewById<Button>(R.id.btnDelete)
        if (onDelete != null) {
            del.visibility = View.VISIBLE
            del.setOnClickListener {
                dialog.dismiss()
                AlertDialog.Builder(ctx)
                    .setTitle("删除「" + rec.word + "」？")
                    .setMessage("只从本 App 的记录里移除，不影响你在不背单词里的进度。")
                    .setPositiveButton("删除") { _, _ -> onDelete() }
                    .setNegativeButton("取消", null)
                    .show()
            }
        } else {
            del.visibility = View.GONE
        }

        dialog.show()
    }

    /** 查近义词：有缓存直接显示，没有才请求 AI（每个词只查一次） */
    private fun showSynonyms(ctx: Context, word: String) {
        val cached = Prefs.synonym(ctx, word)
        if (cached.isNotEmpty()) {
            showResult(ctx, word, cached.split("|").filter { it.isNotBlank() })
            return
        }

        val key = Prefs.apiKey(ctx)
        if (key.isEmpty()) {
            AlertDialog.Builder(ctx)
                .setTitle("近义词")
                .setMessage("这个功能需要用 AI 查一次（之后会记住，不再花钱）。\n先去设置里填 DeepSeek API Key 吧。")
                .setPositiveButton("好", null)
                .show()
            return
        }

        val loading = AlertDialog.Builder(ctx)
            .setMessage("正在查「" + word + "」的近义词…")
            .setCancelable(false)
            .create()
        loading.show()

        Thread {
            var list: List<String> = emptyList()
            var err: String? = null
            try {
                list = SynonymGen.get(ctx, key, word)
            } catch (e: Exception) {
                err = e.message
            }
            Handler(Looper.getMainLooper()).post {
                try {
                    loading.dismiss()
                } catch (e: Exception) {
                }
                if (err != null) {
                    AlertDialog.Builder(ctx)
                        .setTitle("查失败了")
                        .setMessage(err)
                        .setPositiveButton("知道了", null)
                        .show()
                } else if (list.isEmpty()) {
                    AlertDialog.Builder(ctx)
                        .setTitle(word)
                        .setMessage("没找到合适的同义词，换个词试试。")
                        .setPositiveButton("好", null)
                        .show()
                } else {
                    showResult(ctx, word, list)
                }
            }
        }.start()
    }

    private fun showResult(ctx: Context, word: String, list: List<String>) {
        val sb = StringBuilder()
        for (w in list) {
            sb.append("· ").append(w)
            val hit = WordBank.lookup(w)
            if (hit != null && hit.meaning.isNotEmpty()) {
                sb.append("  ").append(hit.meaning)
            }
            sb.append("\n")
        }
        AlertDialog.Builder(ctx)
            .setTitle(word + " 的近义词")
            .setMessage(sb.toString().trim())
            .setPositiveButton("好", null)
            .show()
    }
}
