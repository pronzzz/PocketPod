package io.github.pronzzz.wheelhouse.ui.device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.ViewGroup
import io.github.pronzzz.wheelhouse.ui.screen.ScreenHost
import kotlin.math.max
import kotlin.math.min

/**
 * The whole phone screen drawn as one music player: a case, a screen near the top and the
 * click wheel below. Sizes adapt to anything from a 480×800 budget phone to a tall flagship.
 */
@SuppressLint("ViewConstructor")
class DeviceLayout(context: Context, val screen: ScreenHost, val wheel: ClickWheelView) : ViewGroup(context) {

    var skin: Skin = Skins.all[0]
        set(v) { field = v; dirty = true; invalidate() }

    private val dp = resources.displayMetrics.density
    private val frame = Paint()
    private val sheen = Paint()
    private val bezel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bezelRect = RectF()
    private var dirty = true

    init {
        setWillNotDraw(false)
        // Nothing here uses Android focus. Blocking it stops the first D-pad press after a touch
        // from being swallowed by the system's "leave touch mode" handling.
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        isFocusable = false
        addView(screen)
        addView(wheel)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
        val g = geometry(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom)
        screen.measure(MeasureSpec.makeMeasureSpec(g.screenW, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(g.screenH, MeasureSpec.EXACTLY))
        wheel.measure(MeasureSpec.makeMeasureSpec(g.wheel, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(g.wheel, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val g = geometry(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom)
        val sx = paddingLeft + (width - paddingLeft - paddingRight - g.screenW) / 2
        val sy = paddingTop + g.screenTop
        screen.layout(sx, sy, sx + g.screenW, sy + g.screenH)
        val wx = paddingLeft + (width - paddingLeft - paddingRight - g.wheel) / 2
        val wy = paddingTop + g.wheelTop
        wheel.layout(wx, wy, wx + g.wheel, wy + g.wheel)
        val inset = 5 * dp
        bezelRect.set(sx - inset, sy - inset, sx + g.screenW + inset, sy + g.screenH + inset)
        dirty = true
    }

    private class Geometry(val screenW: Int, val screenH: Int, val screenTop: Int, val wheel: Int, val wheelTop: Int)

    private fun geometry(w: Int, h: Int): Geometry {
        val margin = (max(14 * dp, w * 0.06f)).toInt()
        val screenW = w - 2 * margin
        val wheel = min(w * 0.80f, h * 0.42f).toInt()
        val gapBelowWheel = max(16 * dp, h * 0.04f).toInt()
        val screenTop = margin
        // Screen takes the remaining height, between a 4:3 landscape screen and a square.
        val room = h - screenTop - wheel - gapBelowWheel - margin
        val screenH = room.coerceIn((screenW * 0.72f).toInt(), (screenW * 1.05f).toInt()).coerceAtMost(h - wheel - 2 * margin)
        val below = screenTop + screenH
        val wheelTop = below + max(margin, (h - below - wheel) / 2 - gapBelowWheel / 4)
        return Geometry(screenW, screenH, screenTop, wheel, min(wheelTop, h - wheel - margin / 2))
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (dirty) {
            frame.shader = LinearGradient(0f, 0f, w * 0.3f, h, skin.frameTop, skin.frameBottom, Shader.TileMode.CLAMP)
            sheen.shader = LinearGradient(0f, 0f, w, 0f,
                intArrayOf(0x00FFFFFF, 0x22FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.35f, 0.7f), Shader.TileMode.CLAMP)
            bezel.color = skin.bezel
            dirty = false
        }
        canvas.drawRect(0f, 0f, w, h, frame)
        canvas.drawRect(0f, 0f, w, h, sheen)
        canvas.drawRoundRect(bezelRect, 10 * dp, 10 * dp, bezel)
    }
}
