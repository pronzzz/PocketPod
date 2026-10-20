package io.github.pronzzz.wheelhouse.ui.screen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import io.github.pronzzz.wheelhouse.ui.device.Palette

/** The device screen: a status bar on top and a stack of [Screen]s that slide like an iPod. */
class ScreenHost(context: Context) : ViewGroup(context) {

    val statusBar = StatusBarView(context)
    private val content = FrameLayout(context)
    private val flash = FlashView(context)
    private val stack = ArrayList<Screen>()
    private val dp = resources.displayMetrics.density
    var animate = true
    var palette: Palette = Palette.LIGHT
        set(v) {
            field = v
            statusBar.palette = v
            content.setBackgroundColor(v.bg)
            stack.forEach { it.onPaletteChanged() }
        }

    init {
        addView(content)
        addView(statusBar)
        addView(flash)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 6 * dp)
            }
        }
    }

    val top: Screen? get() = stack.lastOrNull()
    val depth get() = stack.size
    val screens: List<Screen> get() = stack

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val barH = statusBarHeight(h)
        statusBar.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(barH, MeasureSpec.EXACTLY))
        content.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h - barH, MeasureSpec.EXACTLY))
        flash.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val barH = statusBar.measuredHeight
        statusBar.layout(0, 0, r - l, barH)
        content.layout(0, barH, r - l, b - t)
        flash.layout(0, 0, r - l, b - t)
    }

    private fun statusBarHeight(h: Int) = (h * 0.085f).toInt().coerceIn((20 * dp).toInt(), (30 * dp).toInt())

    fun setRoot(screen: Screen) {
        stack.toList().forEach { it.onHidden(); it.onDisposed() }
        stack.clear()
        content.removeAllViews()
        stack += screen
        attach(screen)
        content.addView(screen.view)
        screen.onShown()
        statusBar.title = screen.title
    }

    fun push(screen: Screen) {
        val old = stack.lastOrNull()
        stack += screen
        attach(screen)
        content.addView(screen.view)
        statusBar.title = screen.title
        old?.onHidden()
        screen.onShown()
        slide(incoming = screen.view, outgoing = old?.view, forward = true)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        val old = stack.removeAt(stack.lastIndex)
        val now = stack.last()
        attach(now)
        if (now.view.parent == null) content.addView(now.view, 0)
        statusBar.title = now.title
        old.onHidden()
        now.onShown()
        slide(incoming = now.view, outgoing = old.view, forward = false) { old.onDisposed() }
        return true
    }

    fun popToRoot() {
        while (stack.size > 2) {
            val s = stack.removeAt(stack.lastIndex)
            content.removeView(s.view)
            s.onHidden()
            s.onDisposed()
        }
        pop()
    }

    fun refreshTitle() { top?.let { statusBar.title = it.title } }

    fun flash(message: String) = flash.show(message, palette)

    private fun attach(screen: Screen) {
        val v = screen.view
        v.translationX = 0f
        if (v.layoutParams == null) v.layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private fun slide(incoming: View, outgoing: View?, forward: Boolean, done: () -> Unit = {}) {
        // Settle any transition still running (fast double presses) before starting a new one.
        for (i in content.childCount - 1 downTo 0) {
            val child = content.getChildAt(i)
            child.animate().cancel()
            child.translationX = 0f
            if (child !== incoming && child !== outgoing) content.removeViewAt(i)
        }
        val w = content.width.toFloat()
        if (!animate || w == 0f || outgoing == null) {
            outgoing?.let { content.removeView(it) }
            incoming.translationX = 0f
            done()
            return
        }
        val dir = if (forward) 1 else -1
        incoming.translationX = dir * w
        incoming.animate().translationX(0f).setDuration(DURATION).setInterpolator(DecelerateInterpolator()).start()
        outgoing.animate().translationX(-dir * w).setDuration(DURATION).setInterpolator(DecelerateInterpolator())
            .withEndAction {
                outgoing.translationX = 0f
                content.removeView(outgoing)
                done()
            }.start()
    }

    companion object { const val DURATION = 190L }
}

/** A brief rounded message in the middle of the screen ("Added to Up Next"). */
private class FlashView(context: Context) : View(context) {
    private var message: String? = null
    private val dp = resources.displayMetrics.density
    private val box = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    private val hide = Runnable { message = null; invalidate() }

    init { isClickable = false }

    fun show(msg: String, palette: Palette) {
        message = msg
        box.color = 0xE6202226.toInt()
        text.color = 0xFFFFFFFF.toInt()
        removeCallbacks(hide)
        postDelayed(hide, 1400)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val m = message ?: return
        text.textSize = height * 0.06f
        val tw = text.measureText(m)
        val bw = tw + 32 * dp
        val bh = text.textSize * 2.6f
        rect.set((width - bw) / 2, (height - bh) / 2, (width + bw) / 2, (height + bh) / 2)
        canvas.drawRoundRect(rect, 10 * dp, 10 * dp, box)
        canvas.drawText(m, width / 2f, height / 2f - (text.ascent() + text.descent()) / 2, text)
    }
}
