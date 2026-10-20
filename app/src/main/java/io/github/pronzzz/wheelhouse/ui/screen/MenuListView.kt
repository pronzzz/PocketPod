package io.github.pronzzz.wheelhouse.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Returns a bitmap if one is ready right now, or null (a placeholder is drawn while it loads). */
fun interface ImageSource { fun bitmap(): Bitmap? }

class Row(
    val title: String,
    val subtitle: String? = null,
    val detail: String? = null,
    val chevron: Boolean = false,
    val checked: Boolean = false,
    val playing: Boolean = false,
    val image: ImageSource? = null,
    val selectable: Boolean = true,
    val letter: Char? = null,
    val onSelect: (() -> Unit)? = null,
    val onLong: (() -> Unit)? = null,
)

/** The iPod-style list: one highlighted row, moved by the wheel or by touch. */
@SuppressLint("ViewConstructor")
class MenuListView(private val ui: Ui) : View(ui.activity) {

    var rows: List<Row> = emptyList(); private set
    var selected = 0; private set
    var emptyMessage = "No Items"
    var onActivate: ((Int) -> Unit)? = null
    var onLongActivate: ((Int) -> Unit)? = null
    var onSelectionChanged: ((Int) -> Unit)? = null

    private val dp = ui.dp
    private var tall = false
    private var rowH = 1f
    private var scrollPos = 0f
    private var targetScroll = 0f
    private var selectedSince = 0L
    private var letter: Char? = null
    private var letterUntil = 0L
    private var lastRotateAt = 0L
    private var fastCount = 0
    private var touchMode = false

    private val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val sub = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val detailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val selPaint = Paint()
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val placeholder = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imgPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val divider = Paint()
    private val scrollbar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val letterBox = Paint(Paint.ANTI_ALIAS_FLAG)
    private val letterText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD); textAlign = Paint.Align.CENTER
    }
    private val path = Path()
    private val rect = RectF()
    private val dst = Rect()
    private var selShaderFor = -1f

    private val scroller = OverScroller(context)
    private val artListener: () -> Unit = { invalidate() }

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            touchMode = true
            scrollPos = (scrollPos + dy).coerceIn(0f, maxScroll())
            targetScroll = scrollPos
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            touchMode = true
            scroller.fling(0, scrollPos.toInt(), 0, -vy.toInt(), 0, 0, 0, maxScroll().toInt())
            postInvalidateOnAnimation()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val i = rowAt(e.y) ?: return false
            if (!rows[i].selectable) return false
            setSelected(i, ensureVisible = false)
            onActivate?.invoke(i)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val i = rowAt(e.y) ?: return
            if (!rows[i].selectable) return
            setSelected(i, ensureVisible = false)
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            onLongActivate?.invoke(i)
        }
    })

    init {
        isClickable = true
    }

    fun setRows(newRows: List<Row>, keepSelection: Boolean) {
        val prevTitle = rows.getOrNull(selected)?.title
        rows = newRows
        tall = newRows.any { it.image != null || it.subtitle != null }
        computeRowHeight()
        var sel = if (keepSelection) selected.coerceIn(0, max(0, rows.lastIndex)) else 0
        // If the row we were on moved (e.g. list re-sorted), follow it by title.
        if (keepSelection && prevTitle != null && rows.getOrNull(sel)?.title != prevTitle) {
            rows.indexOfFirst { it.title == prevTitle }.takeIf { it >= 0 }?.let { sel = it }
        }
        if (rows.isNotEmpty() && !rows[sel].selectable) sel = nextSelectable(sel, 1) ?: nextSelectable(sel, -1) ?: sel
        selected = sel
        if (!keepSelection) { scrollPos = 0f; targetScroll = 0f }
        ensureVisible(snap = !keepSelection)
        invalidate()
    }

    fun paletteChanged() {
        selShaderFor = -1f
        invalidate()
    }

    fun selectedRow(): Row? = rows.getOrNull(selected)?.takeIf { it.selectable }

    fun selectIndex(i: Int) = setSelected(i.coerceIn(0, max(0, rows.lastIndex)), ensureVisible = true, snap = true)

    /** Moves the highlight. Returns true if it moved. */
    fun rotate(steps: Int): Boolean {
        if (rows.isEmpty()) return false
        if (touchMode) {
            touchMode = false
            if (!isVisibleRow(selected)) {
                val first = firstVisibleSelectable()
                if (first != null) { setSelected(first, ensureVisible = true); return true }
            }
        }
        var i = selected
        val dir = if (steps > 0) 1 else -1
        repeat(abs(steps)) { nextSelectable(i, dir)?.let { i = it } }
        trackSpeed()
        if (i == selected) return false
        setSelected(i, ensureVisible = true)
        return true
    }

    private fun trackSpeed() {
        val now = SystemClock.uptimeMillis()
        fastCount = if (now - lastRotateAt < 90) fastCount + 1 else 0
        lastRotateAt = now
        val l = rows.getOrNull(selected)?.letter
        if (fastCount > 5 && l != null && rows.size > 40) {
            letter = l
            letterUntil = now + 700
            postInvalidateDelayed(720)
        }
    }

    private fun setSelected(i: Int, ensureVisible: Boolean, snap: Boolean = false) {
        if (i != selected) {
            selected = i
            selectedSince = SystemClock.uptimeMillis()
            onSelectionChanged?.invoke(i)
        }
        if (ensureVisible) ensureVisible(snap)
        invalidate()
    }

    private fun nextSelectable(from: Int, dir: Int): Int? {
        var i = from + dir
        while (i in rows.indices) {
            if (rows[i].selectable) return i
            i += dir
        }
        return null
    }

    private fun isVisibleRow(i: Int) = i * rowH >= scrollPos - 1 && (i + 1) * rowH <= scrollPos + height + 1

    private fun firstVisibleSelectable(): Int? {
        val first = (scrollPos / rowH).toInt().coerceAtLeast(0)
        for (i in first until rows.size) if (rows[i].selectable && isVisibleRow(i)) return i
        return null
    }

    private fun rowAt(y: Float): Int? {
        val i = ((y + scrollPos) / rowH).toInt()
        return i.takeIf { it in rows.indices }
    }

    private fun maxScroll() = max(0f, rows.size * rowH - height)

    private fun computeRowHeight() {
        val h = height.toFloat()
        if (h <= 0f) return
        val target = if (tall) 58 * dp else 36 * dp
        val n = max(if (tall) 3 else 5, (h / target).roundToInt())
        rowH = h / n
    }

    private fun ensureVisible(snap: Boolean) {
        if (height == 0) return
        val top = selected * rowH
        val bottom = top + rowH
        if (top < targetScroll) targetScroll = top
        if (bottom > targetScroll + height) targetScroll = bottom - height
        targetScroll = targetScroll.coerceIn(0f, maxScroll())
        if (snap || ui.liteMode) scrollPos = targetScroll
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        computeRowHeight()
        selShaderFor = -1f
        ensureVisible(snap = true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ui.app.artwork.addListener(artListener)
    }

    override fun onDetachedFromWindow() {
        ui.app.artwork.removeListener(artListener)
        super.onDetachedFromWindow()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = gestures.onTouchEvent(event) || super.onTouchEvent(event)

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollPos = scroller.currY.toFloat().coerceIn(0f, maxScroll())
            targetScroll = scrollPos
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val p = ui.palette
        val w = width.toFloat()
        canvas.drawColor(p.bg)
        if (rows.isEmpty()) {
            drawEmpty(canvas)
            return
        }
        // Ease towards the target scroll position.
        if (abs(targetScroll - scrollPos) > 0.5f) {
            scrollPos += (targetScroll - scrollPos) * 0.38f
            postInvalidateOnAnimation()
        } else {
            scrollPos = targetScroll
        }

        if (selShaderFor != rowH) {
            selPaint.shader = LinearGradient(0f, 0f, 0f, rowH, p.selTop, p.selBottom, Shader.TileMode.CLAMP)
            selShaderFor = rowH
        }
        title.textSize = if (tall) rowH * 0.27f else rowH * 0.43f
        sub.textSize = rowH * 0.22f
        detailPaint.textSize = if (tall) rowH * 0.22f else rowH * 0.36f
        divider.color = p.divider

        val needsBar = rows.size * rowH > height + 1
        val rightPad = if (needsBar) 10 * dp else 0f
        val first = (scrollPos / rowH).toInt().coerceAtLeast(0)
        val last = ((scrollPos + height) / rowH).toInt().coerceAtMost(rows.lastIndex)
        var marqueeRunning = false
        for (i in first..last) {
            val top = i * rowH - scrollPos
            if (drawRow(canvas, rows[i], i == selected, top, w - rightPad)) marqueeRunning = true
        }
        if (needsBar) drawScrollbar(canvas)
        drawLetter(canvas)
        if (marqueeRunning) postInvalidateOnAnimation()
    }

    /** Draws one row; returns true if its title is scrolling (needs another frame). */
    private fun drawRow(c: Canvas, row: Row, isSel: Boolean, top: Float, right: Float): Boolean {
        val p = ui.palette
        val pad = 8 * dp
        val ink = if (isSel) p.selText else if (row.selectable) p.text else p.secondary
        val subInk = if (isSel) (p.selText and 0x00FFFFFF) or 0xD0000000.toInt() else p.secondary
        if (isSel) {
            c.save()
            c.translate(0f, top)
            c.drawRect(0f, 0f, right, rowH, selPaint)
            c.restore()
        }
        var x = pad
        // Artwork / icon
        if (row.image != null) {
            val s = rowH - 8 * dp
            val y = top + 4 * dp
            val bmp = row.image.bitmap()
            if (bmp != null) {
                dst.set(x.toInt(), y.toInt(), (x + s).toInt(), (y + s).toInt())
                c.drawBitmap(bmp, null, dst, imgPaint)
            } else {
                drawPlaceholder(c, x, y, s)
            }
            x += s + pad
        }
        // Right side: chevron / check / speaker / detail text
        var r = right - pad
        if (row.chevron) {
            icon.color = ink
            icon.style = Paint.Style.STROKE
            icon.strokeWidth = 2 * dp
            val cy = top + rowH / 2
            val s = rowH * if (tall) 0.1f else 0.15f
            path.reset()
            path.moveTo(r - s, cy - s * 1.4f); path.lineTo(r, cy); path.lineTo(r - s, cy + s * 1.4f)
            c.drawPath(path, icon)
            icon.style = Paint.Style.FILL
            r -= s + pad
        }
        if (row.checked) {
            icon.color = ink
            icon.style = Paint.Style.STROKE
            icon.strokeWidth = 2.2f * dp
            val cy = top + rowH / 2
            val s = rowH * if (tall) 0.11f else 0.16f
            path.reset()
            path.moveTo(r - s * 2f, cy); path.lineTo(r - s * 1.2f, cy + s * 0.9f); path.lineTo(r, cy - s * 1.1f)
            c.drawPath(path, icon)
            icon.style = Paint.Style.FILL
            r -= s * 2f + pad
        }
        if (row.playing) {
            icon.color = ink
            val cy = top + rowH / 2
            val s = rowH * if (tall) 0.09f else 0.13f
            c.drawRect(r - s * 2.4f, cy - s * 0.55f, r - s * 1.7f, cy + s * 0.55f, icon)
            path.reset()
            path.moveTo(r - s * 1.7f, cy - s * 0.55f); path.lineTo(r - s * 0.6f, cy - s * 1.3f)
            path.lineTo(r - s * 0.6f, cy + s * 1.3f); path.lineTo(r - s * 1.7f, cy + s * 0.55f); path.close()
            c.drawPath(path, icon)
            r -= s * 2.4f + pad
        }
        row.detail?.let { d ->
            detailPaint.color = if (isSel) p.selText else p.secondary
            val dw = detailPaint.measureText(d)
            val ty = if (tall) top + rowH * 0.5f else top + rowH / 2
            c.drawText(d, r - dw, ty - (detailPaint.ascent() + detailPaint.descent()) / 2, detailPaint)
            r -= dw + pad
        }

        // Title (+ subtitle)
        val avail = max(0f, r - x)
        title.color = ink
        val titleY = if (row.subtitle != null) top + rowH * 0.40f else top + rowH / 2
        val baseline = titleY - (title.ascent() + title.descent()) / 2
        var marquee = false
        val tw = title.measureText(row.title)
        if (isSel && tw > avail && !ui.liteMode) {
            marquee = true
            val elapsed = SystemClock.uptimeMillis() - selectedSince - 900
            val gap = 48 * dp
            val offset = if (elapsed <= 0) 0f else ((elapsed * 0.045f * dp) % (tw + gap))
            c.save()
            c.clipRect(x, top, x + avail, top + rowH)
            c.drawText(row.title, x - offset, baseline, title)
            if (offset > 0) c.drawText(row.title, x - offset + tw + gap, baseline, title)
            c.restore()
        } else {
            c.drawText(ellipsize(row.title, title, avail), x, baseline, title)
        }
        row.subtitle?.let { s ->
            sub.color = subInk
            val sy = top + rowH * 0.70f - (sub.ascent() + sub.descent()) / 2
            c.drawText(ellipsize(s, sub, avail), x, sy, sub)
        }
        if (!isSel && tall) c.drawRect(x, top + rowH - dp * 0.6f, right, top + rowH, divider)
        return marquee
    }

    private fun drawPlaceholder(c: Canvas, x: Float, y: Float, s: Float) {
        val p = ui.palette
        placeholder.shader = LinearGradient(0f, y, 0f, y + s, p.placeholderTop, p.placeholderBottom, Shader.TileMode.CLAMP)
        rect.set(x, y, x + s, y + s)
        c.drawRect(rect, placeholder)
        // A simple quaver
        icon.color = 0xAAFFFFFF.toInt()
        val cx = x + s * 0.45f
        val cy = y + s * 0.66f
        c.drawCircle(cx, cy, s * 0.11f, icon)
        c.drawRect(cx + s * 0.08f, y + s * 0.25f, cx + s * 0.13f, cy, icon)
        c.drawRect(cx + s * 0.08f, y + s * 0.25f, cx + s * 0.28f, y + s * 0.32f, icon)
    }

    private fun drawScrollbar(c: Canvas) {
        val p = ui.palette
        val w = width.toFloat()
        val trackW = 6 * dp
        val left = w - trackW - 2 * dp
        scrollbar.color = p.divider
        rect.set(left, 2 * dp, left + trackW, height - 2 * dp)
        c.drawRoundRect(rect, trackW / 2, trackW / 2, scrollbar)
        val total = rows.size * rowH
        val thumbH = max(16 * dp, (height - 4 * dp) * height / total)
        val t = scrollPos / (total - height).coerceAtLeast(1f)
        val thumbTop = 2 * dp + (height - 4 * dp - thumbH) * t.coerceIn(0f, 1f)
        scrollbar.shader = LinearGradient(left, 0f, left + trackW, 0f, p.selTop, p.selBottom, Shader.TileMode.CLAMP)
        rect.set(left, thumbTop, left + trackW, thumbTop + thumbH)
        c.drawRoundRect(rect, trackW / 2, trackW / 2, scrollbar)
        scrollbar.shader = null
    }

    private fun drawLetter(c: Canvas) {
        val l = letter ?: return
        if (SystemClock.uptimeMillis() > letterUntil) { letter = null; return }
        val s = minOf(width, height) * 0.32f
        rect.set((width - s) / 2, (height - s) / 2, (width + s) / 2, (height + s) / 2)
        letterBox.color = 0xD9202226.toInt()
        c.drawRoundRect(rect, 12 * dp, 12 * dp, letterBox)
        letterText.color = 0xFFFFFFFF.toInt()
        letterText.textSize = s * 0.6f
        c.drawText(l.toString(), width / 2f, height / 2f - (letterText.ascent() + letterText.descent()) / 2, letterText)
    }

    private fun drawEmpty(c: Canvas) {
        val p = ui.palette
        title.color = p.secondary
        title.textSize = 15 * dp
        title.textAlign = Paint.Align.CENTER
        val lines = emptyMessage.split('\n')
        val lh = title.textSize * 1.4f
        var y = height / 2f - (lines.size - 1) * lh / 2 - (title.ascent() + title.descent()) / 2
        for (line in lines) {
            c.drawText(ellipsize(line, title, width - 24 * dp), width / 2f, y, title)
            y += lh
        }
        title.textAlign = Paint.Align.LEFT
    }

    private fun ellipsize(s: String, paint: TextPaint, w: Float): String =
        if (paint.measureText(s) <= w) s else TextUtils.ellipsize(s, paint, w, TextUtils.TruncateAt.END).toString()
}
