package com.shilin.cuoci

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 背景漂浮粒子：给界面一点"活"的感觉，不影响操作。
 */
class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private class Dot(
        var x: Float,
        var y: Float,
        var r: Float,
        var vx: Float,
        var vy: Float,
        var alpha: Int
    )

    private val dots = ArrayList<Dot>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ticking = false

    private val ticker = object : Runnable {
        override fun run() {
            update()
            invalidate()
            postDelayed(this, 50L)
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        dots.clear()
        if (w == 0 || h == 0) return
        val count = 28
        for (i in 0 until count) {
            val angle = Random.nextFloat() * Math.PI * 2
            val speed = dp(0.12f) + Random.nextFloat() * dp(0.25f)
            dots.add(
                Dot(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    r = dp(2f) + Random.nextFloat() * dp(4f),
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    alpha = 45 + Random.nextInt(55)
                )
            )
        }
    }

    private fun update() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        for (d in dots) {
            d.x += d.vx
            d.y += d.vy
            if (d.x < -d.r) d.x = w + d.r
            if (d.x > w + d.r) d.x = -d.r
            if (d.y < -d.r) d.y = h + d.r
            if (d.y > h + d.r) d.y = -d.r
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val color = runCatching { SkinManager.brand(context) }.getOrDefault(0xFF2F4FE0.toInt())
        for (d in dots) {
            paint.color = color
            paint.alpha = d.alpha
            canvas.drawCircle(d.x, d.y, d.r, paint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    private fun start() {
        if (ticking) return
        ticking = true
        post(ticker)
    }

    private fun stop() {
        ticking = false
        removeCallbacks(ticker)
    }
}
