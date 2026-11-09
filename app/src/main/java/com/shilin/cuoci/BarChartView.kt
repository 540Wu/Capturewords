package com.shilin.cuoci

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 简单的柱状图，用在「我的」页的学习时长统计。
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var labels: List<String> = emptyList()
    private var values: List<Long> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var barColor = 0xFF2F4FE0.toInt()
    private var trackColor = 0x14000000

    init {
        textPaint.textSize = dp(9f)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = 0xFF999999.toInt()
    }

    fun setData(items: List<Pair<String, Long>>) {
        labels = items.map { it.first }
        values = items.map { it.second }
        invalidate()
    }

    fun setColors(bar: Int, track: Int, label: Int) {
        barColor = bar
        trackColor = track
        textPaint.color = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = values.size
        if (n == 0) return

        val labelH = textPaint.textSize * 1.8f
        val top = dp(6f)
        val bottom = height - labelH
        val chartH = bottom - top
        if (chartH <= dp(10f)) return

        var maxV = values.maxOrNull() ?: 0L
        if (maxV < 60_000L) maxV = 60_000L

        val slot = width.toFloat() / n
        val barW = minOf(slot * 0.55f, dp(30f))
        val radius = dp(3f)

        barPaint.color = barColor
        trackPaint.color = trackColor

        for (i in 0 until n) {
            val cx = slot * i + slot / 2f
            val v = values[i]
            val frac = (v.toFloat() / maxV).coerceIn(0f, 1f)
            val h = chartH * frac

            // 背景轨道
            canvas.drawRoundRect(
                RectF(cx - barW / 2, top, cx + barW / 2, bottom),
                radius, radius, trackPaint
            )

            // 柱子
            if (h > dp(1f)) {
                canvas.drawRoundRect(
                    RectF(cx - barW / 2, bottom - h, cx + barW / 2, bottom),
                    radius, radius, barPaint
                )
            }

            // 标签（太密就隔几个画一个）
            val step = when {
                n <= 8 -> 1
                n <= 15 -> 2
                else -> 5
            }
            if (i % step == 0 || i == n - 1) {
                canvas.drawText(labels[i], cx, height - dp(3f), textPaint)
            }
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
