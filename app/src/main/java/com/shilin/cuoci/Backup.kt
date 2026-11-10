package com.shilin.cuoci

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

/**
 * 数据备份：导出成一个 JSON 文件（可用微信传），另一台设备导入后合并。
 */
object Backup {

    class MergeResult(val words: Int, val essays: Int)

    fun export(ctx: Context): File {
        val f = File(ctx.cacheDir, "错词捕手备份-" + DayUtil.todayKey() + ".json")
        f.writeText(exportJson(), Charsets.UTF_8)
        return f
    }

    /** 导出成 JSON 字符串（局域网直传用） */
    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "cuoci")
        root.put("version", 1)
        root.put("exportAt", System.currentTimeMillis())
        root.put("records", WordStore.exportData())
        root.put("essays", EssayStore.exportData())
        root.put("usage", UsageStore.exportData())
        root.put("pet", PetStore.exportData())
        return root.toString()
    }

    fun share(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val i = Intent(Intent.ACTION_SEND)
        i.type = "application/json"
        i.putExtra(Intent.EXTRA_STREAM, uri)
        i.putExtra(Intent.EXTRA_SUBJECT, "错词捕手数据备份")
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "发送备份文件"))
    }

    fun import(ctx: Context, text: String): MergeResult {
        val root = JSONObject(text)
        val words = WordStore.mergeData(root.optJSONObject("records") ?: JSONObject())
        val essays = EssayStore.mergeData(root.optJSONObject("essays") ?: JSONObject())
        UsageStore.mergeData(root.optJSONObject("usage") ?: JSONObject())
        PetStore.mergeData(root.optJSONObject("pet") ?: JSONObject())
        return MergeResult(words, essays)
    }

    fun readText(ctx: Context, uri: Uri): String {
        val input = ctx.contentResolver.openInputStream(uri)
            ?: throw RuntimeException("打不开这个文件")
        return input.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
