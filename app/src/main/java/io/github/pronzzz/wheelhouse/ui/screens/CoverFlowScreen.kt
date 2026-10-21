package io.github.pronzzz.wheelhouse.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import io.github.pronzzz.wheelhouse.data.Album
import io.github.pronzzz.wheelhouse.data.ArtworkLoader
import io.github.pronzzz.wheelhouse.ui.screen.Screen
import io.github.pronzzz.wheelhouse.ui.screen.Ui
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

class CoverFlowScreen(ui: Ui) : Screen(ui) {

    override val title = "Cover Flow"
    private var albums: List<Album> = ui.app.library.index.value.albums
    private var target = 0
    private var pos = 0f
    private lateinit var flow: FlowView

    override fun createView(): View = FlowView().also { flow = it }

    override fun onRotate(steps: Int) {
        if (albums.isEmpty()) return
        target = (target + steps).coerceIn(0, albums.lastIndex)
        flow.invalidate()
    }

    override fun onSelect() {
        val a = albums.getOrNull(target) ?: return
        ui.push(Menus.albumSongs(ui, a))
    }

    override fun onSelectLong() {
        val a = albums.getOrNull(target) ?: return
        ui.app.player.playTracks(a.tracks, 0, shuffle = false)
        ui.showNowPlaying()
    }

    override fun onLibraryChanged() {
        val currentId = albums.getOrNull(target)?.albumId
        albums = ui.app.library.index.value.albums
        target = albums.indexOfFirst { it.albumId == currentId }.coerceAtLeast(0)
        pos = target.toFloat()
        if (::flow.isInitialized) flow.invalidate()
    }

    override val allowsIdleNowPlaying get() = false

    @SuppressLint("ViewConstructor")
    private inner class FlowView : View(ui.activity) {
        private val dp = ui.dp
        private val m = Matrix()
        private val src = FloatArray(8)
        private val dst = FloatArray(8)
        private val img = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val reflect = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply { alpha = 80 }
        private val fade = Paint()
        private var fadeFor = -1f
        private val ph = Paint(Paint.ANTI_ALIAS_FLAG)
        private val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD); textAlign = Paint.Align.CENTER; color = 0xFFFFFFFF.toInt()
        }
        private val subP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = 0xFFB4B8BF.toInt() }
        private val artListener: () -> Unit = { invalidate() }
        private var placeholder: Bitmap? = null
        private var downX = 0f
        private var downPos = 0f
        private var dragging = false

        override fun onAttachedToWindow() { super.onAttachedToWindow(); ui.app.artwork.addListener(artListener) }
        override fun onDetachedFromWindow() { ui.app.artwork.removeListener(artListener); super.onDetachedFromWindow() }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            val cover = coverSize()
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.x; downPos = pos; dragging = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.x - downX
                    if (abs(dx) > 8 * dp) dragging = true
                    if (dragging && albums.isNotEmpty()) {
                        pos = (downPos - dx / (cover * 0.45f)).coerceIn(0f, albums.lastIndex.toFloat())
                        target = pos.roundToInt()
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        target = pos.roundToInt().coerceIn(0, (albums.size - 1).coerceAtLeast(0))
                    } else {
                        val dx = e.x - width / 2f
                        if (abs(dx) < cover / 2) onSelect() else onRotate(dx.sign.toInt())
                    }
                    invalidate()
                }
            }
            return true
        }

        private fun coverSize() = min(width * 0.52f, height * 0.56f)

        override fun onDraw(c: Canvas) {
            c.drawColor(0xFF000000.toInt())
            if (albums.isEmpty()) {
                subP.textSize = height * 0.06f
                c.drawText("No Albums", width / 2f, height / 2f, subP)
                return
            }
            val diff = target - pos
            if (abs(diff) > 0.002f) {
                pos += if (ui.liteMode) diff else diff * 0.22f
                postInvalidateOnAnimation()
            } else pos = target.toFloat()

            val s = coverSize()
            val cx = width / 2f
            val cy = height * 0.40f
            val range = if (ui.liteMode) 3 else 5
            val first = (pos - range).toInt().coerceAtLeast(0)
            val last = (pos + range).toInt().coerceAtMost(albums.lastIndex)
            val order = (first..last).sortedByDescending { abs(it - pos) }
            for (i in order) drawCover(c, albums[i], i - pos, cx, cy, s)

            // Fade the reflections into the floor.
            val floor = cy + s / 2
            if (fadeFor != floor) {
                fade.shader = LinearGradient(0f, floor, 0f, floor + s * 0.4f, 0x33000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP)
                fadeFor = floor
            }
            c.drawRect(0f, floor, width.toFloat(), height.toFloat(), fade)

            val a = albums[target.coerceIn(0, albums.lastIndex)]
            titleP.textSize = height * 0.065f
            subP.textSize = height * 0.055f
            val ty = height - height * 0.13f
            c.drawText(ell(a.title, titleP), cx, ty, titleP)
            c.drawText(ell(a.artist, subP), cx, ty + subP.textSize * 1.4f, subP)
        }

        private fun ell(s: String, p: TextPaint) = TextUtils.ellipsize(s, p, width - 24 * dp, TextUtils.TruncateAt.END).toString()

        private fun drawCover(c: Canvas, album: Album, d: Float, cx: Float, cy: Float, s: Float) {
            val bmp = ui.app.artwork.album(album.albumId, album.tracks.firstOrNull(),
                if (ui.liteMode) ArtworkLoader.Quality.LITE_LARGE else ArtworkLoader.Quality.LARGE) ?: placeholder()
            val t = min(1f, abs(d))
            val side = sign(d)
            val spread = s * 0.72f
            val x = if (abs(d) <= 1f) cx + d * spread else cx + side * (spread + (abs(d) - 1f) * s * 0.26f)
            val pw = s * lerp(1f, 0.42f, t)
            // Side covers turn to face the centre, so their outer edge is nearer (taller).
            val outerH = s * lerp(1f, 0.94f, t)
            val innerH = s * lerp(1f, 0.70f, t)
            val leftH = if (side < 0) outerH else innerH
            val rightH = if (side < 0) innerH else outerH
            val l = x - pw / 2
            val r = x + pw / 2
            val bw = bmp.width.toFloat()
            val bh = bmp.height.toFloat()
            src[0] = 0f; src[1] = 0f; src[2] = bw; src[3] = 0f; src[4] = bw; src[5] = bh; src[6] = 0f; src[7] = bh
            dst[0] = l; dst[1] = cy - leftH / 2; dst[2] = r; dst[3] = cy - rightH / 2
            dst[4] = r; dst[5] = cy + rightH / 2; dst[6] = l; dst[7] = cy + leftH / 2
            m.setPolyToPoly(src, 0, dst, 0, 4)
            c.drawBitmap(bmp, m, img)
            if (!ui.liteMode) {
                // Mirror image hanging below the cover.
                dst[0] = l; dst[1] = cy + leftH / 2 + leftH; dst[2] = r; dst[3] = cy + rightH / 2 + rightH
                dst[4] = r; dst[5] = cy + rightH / 2; dst[6] = l; dst[7] = cy + leftH / 2
                m.setPolyToPoly(src, 0, dst, 0, 4)
                c.drawBitmap(bmp, m, reflect)
            }
        }

        private fun placeholder(): Bitmap = placeholder ?: Bitmap.createBitmap(64, 64, Bitmap.Config.RGB_565).also { b ->
            val cv = Canvas(b)
            ph.shader = LinearGradient(0f, 0f, 0f, 64f, 0xFF4A5059.toInt(), 0xFF1E2227.toInt(), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, 64f, 64f, ph)
            ph.shader = null
            ph.color = 0x99FFFFFF.toInt()
            cv.drawCircle(28f, 42f, 7f, ph)
            cv.drawRect(33f, 16f, 36f, 42f, ph)
            cv.drawRect(33f, 16f, 46f, 21f, ph)
            placeholder = b
        }

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }
}
