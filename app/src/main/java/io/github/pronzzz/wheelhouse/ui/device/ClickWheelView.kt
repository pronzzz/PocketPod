package io.github.pronzzz.wheelhouse.ui.device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

interface WheelListener {
    fun onWheelRotate(steps: Int)
    fun onWheelButton(button: WheelButton)
    fun onWheelLongPress(button: WheelButton)
    fun onWheelHoldEnd(button: WheelButton)
}

/** A touch click wheel: drag around the ring to scroll, tap the four labels or the centre button. */
class ClickWheelView(context: Context, private val feedback: Feedback) : View(context) {

    var listener: WheelListener? = null
    var skin: Skin = Skins.all[0]
        set(v) { field = v; shadersDirty = true; invalidate() }
    var sensitivity: Int = 1
        set(v) { field = v; accumulator = RotationAccumulator(Math.toRadians(WheelMath.stepDegrees(v))) }

    private var accumulator = RotationAccumulator(Math.toRadians(WheelMath.stepDegrees(1)))
    private val handler = Handler(Looper.getMainLooper())
    private val dp = resources.displayMetrics.density

    // Touch state
    private var downButton: WheelButton? = null
    private var pressedButton: WheelButton? = null
    private var lastAngle = 0.0
    private var travelled = 0.0
    private var signedTravel = 0.0
    private var rotating = false
    private var longFired = false
    private var holding = false

    // Geometry
    private var cx = 0f
    private var cy = 0f
    private var outerR = 0f
    private var centerR = 0f

    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var shadersDirty = true

    private val longPress = Runnable {
        val b = downButton ?: return@Runnable
        if (rotating) return@Runnable
        longFired = true
        feedback.press(this)
        listener?.onWheelLongPress(b)
        if (b == WheelButton.PREV || b == WheelButton.NEXT) holding = true
    }

    init {
        isClickable = true
        isHapticFeedbackEnabled = true
        contentDescription = "Click wheel. Drag around the ring to scroll. Tap Menu, Previous, Next, Play or the centre button."
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        outerR = min(w, h) / 2f - 2 * dp
        centerR = outerR * 0.37f
        shadersDirty = true
    }

    private fun buildShaders() {
        val s = skin
        wheelPaint.shader = RadialGradient(cx, cy - outerR * 0.3f, outerR * 1.4f,
            lighten(s.wheel, 0.06f), darken(s.wheel, 0.05f), Shader.TileMode.CLAMP)
        edgePaint.color = s.wheelEdge
        edgePaint.strokeWidth = 1.2f * dp
        centerPaint.shader = LinearGradient(0f, cy - centerR, 0f, cy + centerR, s.centerTop, s.centerBottom, Shader.TileMode.CLAMP)
        centerEdgePaint.color = s.centerEdge
        centerEdgePaint.strokeWidth = 1.2f * dp
        labelPaint.color = s.wheelLabel
        labelPaint.textSize = outerR * 0.115f
        labelPaint.letterSpacing = 0.08f
        iconPaint.color = s.wheelLabel
        pressPaint.color = if (s.darkChrome) 0x22FFFFFF else 0x14000000
        shadersDirty = false
    }

    override fun onDraw(canvas: Canvas) {
        if (shadersDirty) buildShaders()
        canvas.drawCircle(cx, cy, outerR, wheelPaint)
        canvas.drawCircle(cx, cy, outerR, edgePaint)

        pressedButton?.takeIf { it != WheelButton.SELECT }?.let { drawPressedSector(canvas, it) }

        // MENU
        val labelY = cy - outerR * 0.68f - (labelPaint.descent() + labelPaint.ascent()) / 2
        canvas.drawText("MENU", cx, labelY, labelPaint)
        val u = outerR * 0.055f
        // Previous ⏮ (left)
        drawSkip(canvas, cx - outerR * 0.70f, cy, u, forward = false)
        // Next ⏭ (right)
        drawSkip(canvas, cx + outerR * 0.70f, cy, u, forward = true)
        // Play/Pause ⏯ (bottom)
        drawPlayPause(canvas, cx, cy + outerR * 0.70f, u)

        canvas.drawCircle(cx, cy, centerR, centerPaint)
        canvas.drawCircle(cx, cy, centerR, centerEdgePaint)
        if (pressedButton == WheelButton.SELECT) canvas.drawCircle(cx, cy, centerR, pressPaint)
    }

    private fun drawPressedSector(canvas: Canvas, b: WheelButton) {
        val start = when (b) {
            WheelButton.MENU -> -135f
            WheelButton.NEXT -> -45f
            WheelButton.PLAY -> 45f
            else -> 135f
        }
        path.reset()
        path.arcTo(cx - outerR, cy - outerR, cx + outerR, cy + outerR, start, 90f, true)
        path.arcTo(cx - centerR, cy - centerR, cx + centerR, cy + centerR, start + 90f, -90f, false)
        path.close()
        canvas.drawPath(path, pressPaint)
    }

    private fun drawSkip(canvas: Canvas, x: Float, y: Float, u: Float, forward: Boolean) {
        val dir = if (forward) 1 else -1
        val w = 1.7f * u
        val h = 1.6f * u
        // Two triangles and a bar
        for (k in 0..1) {
            val baseX = x - dir * w + dir * k * w
            path.reset()
            path.moveTo(baseX, y - h)
            path.lineTo(baseX + dir * w, y)
            path.lineTo(baseX, y + h)
            path.close()
            canvas.drawPath(path, iconPaint)
        }
        val barX = x + dir * w
        val bw = 0.45f * u
        if (forward) canvas.drawRect(barX, y - h, barX + bw, y + h, iconPaint)
        else canvas.drawRect(barX - bw, y - h, barX, y + h, iconPaint)
    }

    private fun drawPlayPause(canvas: Canvas, x: Float, y: Float, u: Float) {
        val h = 1.5f * u
        val left = x - 2.3f * u
        path.reset()
        path.moveTo(left, y - h)
        path.lineTo(left + 1.9f * u, y)
        path.lineTo(left, y + h)
        path.close()
        canvas.drawPath(path, iconPaint)
        val bw = 0.55f * u
        val p1 = x + 0.45f * u
        canvas.drawRect(p1, y - h, p1 + bw, y + h, iconPaint)
        val p2 = p1 + bw + 0.6f * u
        canvas.drawRect(p2, y - h, p2 + bw, y + h, iconPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val dx = e.x - cx
        val dy = e.y - cy
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val b = WheelMath.buttonAt(dx, dy, outerR, centerR) ?: return false
                downButton = b
                pressedButton = b
                rotating = false
                longFired = false
                holding = false
                travelled = 0.0
                signedTravel = 0.0
                lastAngle = WheelMath.angle(dx, dy)
                accumulator.reset()
                handler.postDelayed(longPress, LONG_PRESS_MS)
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val b = downButton ?: return true
                if (b == WheelButton.SELECT && !rotating) {
                    // Allow sliding off the centre button onto the ring to start scrolling.
                    if (hypot(dx, dy) <= centerR * 1.15f) return true
                    lastAngle = WheelMath.angle(dx, dy)
                }
                if (hypot(dx, dy) < centerR * 0.5f) return true // too close to the centre to read an angle
                val a = WheelMath.angle(dx, dy)
                val d = WheelMath.delta(lastAngle, a)
                lastAngle = a
                travelled += kotlin.math.abs(d)
                signedTravel += d
                var feed = d
                if (!rotating && travelled > Math.toRadians(ROTATE_SLOP_DEG) && !longFired) {
                    rotating = true
                    pressedButton = null
                    handler.removeCallbacks(longPress)
                    feed = signedTravel // the slop counts too, so short turns feel responsive
                    invalidate()
                }
                if (rotating) {
                    val steps = accumulator.feed(feed)
                    if (steps != 0) {
                        feedback.tick(this)
                        listener?.onWheelRotate(steps)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                val b = downButton
                if (b != null && !rotating) {
                    if (longFired) {
                        if (holding) listener?.onWheelHoldEnd(b)
                    } else {
                        feedback.press(this)
                        listener?.onWheelButton(b)
                    }
                }
                reset()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                if (holding) downButton?.let { listener?.onWheelHoldEnd(it) }
                reset()
            }
        }
        return true
    }

    private fun reset() {
        downButton = null
        pressedButton = null
        rotating = false
        holding = false
        invalidate()
    }

    private fun lighten(c: Int, f: Float) = blend(c, 0xFFFFFFFF.toInt(), f)
    private fun darken(c: Int, f: Float) = blend(c, 0xFF000000.toInt(), f)

    companion object {
        const val LONG_PRESS_MS = 550L
        const val ROTATE_SLOP_DEG = 9.0

        fun blend(a: Int, b: Int, f: Float): Int {
            val ia = 1 - f
            val r = (((a shr 16) and 0xFF) * ia + ((b shr 16) and 0xFF) * f).toInt()
            val g = (((a shr 8) and 0xFF) * ia + ((b shr 8) and 0xFF) * f).toInt()
            val bl = ((a and 0xFF) * ia + (b and 0xFF) * f).toInt()
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
        }
    }
}
