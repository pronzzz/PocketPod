package io.github.pronzzz.wheelhouse.ui.screen

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextUtils
import android.text.TextPaint
import android.view.View
import io.github.pronzzz.wheelhouse.ui.device.Palette

/** The bar at the top of the screen: play state, title, battery. */
class StatusBarView(context: Context) : View(context) {

    var palette: Palette = Palette.LIGHT
        set(v) { field = v; shaderDirty = true; invalidate() }
    var title: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var playState: Int = 0 // 0 none, 1 playing, 2 paused
        set(v) { if (field != v) { field = v; invalidate() } }
    var sleeping = false
        set(v) { if (field != v) { field = v; invalidate() } }
    var battery = -1
        set(v) { if (field != v) { field = v; invalidate() } }
    var charging = false
        set(v) { if (field != v) { field = v; invalidate() } }

    private val dp = resources.displayMetrics.density
    private val bg = Paint()
    private val line = Paint()
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private var shaderDirty = true

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { shaderDirty = true }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (shaderDirty) {
            bg.shader = LinearGradient(0f, 0f, 0f, h, palette.statusTop, palette.statusBottom, Shader.TileMode.CLAMP)
            line.color = palette.secondary
            shaderDirty = false
        }
        canvas.drawRect(0f, 0f, w, h, bg)
        canvas.drawRect(0f, h - dp * 0.75f, w, h, line)

        val ink = palette.statusText
        icon.color = ink
        text.color = ink
        text.textSize = h * 0.5f

        // Left: play state (+ sleep "z")
        val ix = h * 0.42f
        val cy = h / 2f
        val s = h * 0.2f
        when (playState) {
            1 -> {
                path.reset()
                path.moveTo(ix, cy - s); path.lineTo(ix + s * 1.6f, cy); path.lineTo(ix, cy + s); path.close()
                canvas.drawPath(path, icon)
            }
            2 -> {
                canvas.drawRect(ix, cy - s, ix + s * 0.55f, cy + s, icon)
                canvas.drawRect(ix + s * 1.05f, cy - s, ix + s * 1.6f, cy + s, icon)
            }
        }
        if (sleeping) {
            val old = text.textSize
            text.textSize = h * 0.42f
            canvas.drawText("z", ix + s * 2.4f, cy - (text.ascent() + text.descent()) / 2, text)
            text.textSize = old
        }

        // Right: battery
        val bw = h * 0.95f
        val bh = h * 0.42f
        val right = w - h * 0.4f
        rect.set(right - bw, cy - bh / 2, right - bw * 0.1f, cy + bh / 2)
        stroke.color = ink
        stroke.strokeWidth = dp
        canvas.drawRoundRect(rect, dp * 2, dp * 2, stroke)
        canvas.drawRect(rect.right, cy - bh * 0.22f, right, cy + bh * 0.22f, icon)
        if (battery >= 0) {
            val inset = dp * 1.8f
            val fw = (rect.width() - inset * 2) * (battery / 100f)
            fill.shader = null
            fill.color = when {
                charging -> 0xFF35C759.toInt()
                battery <= 15 -> 0xFFE5484D.toInt()
                else -> 0xFF4CD964.toInt()
            }
            canvas.drawRect(rect.left + inset, rect.top + inset, rect.left + inset + fw, rect.bottom - inset, fill)
            if (charging) {
                val bx = rect.centerX()
                path.reset()
                path.moveTo(bx + bh * 0.1f, rect.top - dp)
                path.lineTo(bx - bh * 0.35f, cy + bh * 0.08f)
                path.lineTo(bx - bh * 0.02f, cy + bh * 0.08f)
                path.lineTo(bx - bh * 0.12f, rect.bottom + dp)
                path.lineTo(bx + bh * 0.35f, cy - bh * 0.08f)
                path.lineTo(bx + bh * 0.02f, cy - bh * 0.08f)
                path.close()
                canvas.drawPath(path, icon)
            }
        }

        // Centre: title
        val maxW = w - 2 * (bw + h * 0.9f)
        val t = TextUtils.ellipsize(title, text, maxW, TextUtils.TruncateAt.END).toString()
        val tw = text.measureText(t)
        canvas.drawText(t, (w - tw) / 2, cy - (text.ascent() + text.descent()) / 2, text)
    }
}
