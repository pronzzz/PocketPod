package io.github.pronzzz.wheelhouse.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioManager
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import io.github.pronzzz.wheelhouse.data.ArtworkLoader
import io.github.pronzzz.wheelhouse.ui.screen.Screen
import io.github.pronzzz.wheelhouse.ui.screen.Ui
import io.github.pronzzz.wheelhouse.util.Text
import kotlin.math.max
import kotlin.math.min

class NowPlayingScreen(ui: Ui) : Screen(ui) {

    enum class Mode { NORMAL, SCRUB, SHUFFLE, REPEAT }

    override val title = "Now Playing"
    override val isNowPlaying = true

    private var mode = Mode.NORMAL
    private var modeTouchedAt = 0L
    private var volumeUntil = 0L
    private var scrubTarget = -1L
    private var scrubCommitAt = 0L
    private lateinit var npView: NowPlayingView

    private val audio = ui.activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val player get() = ui.app.player

    override fun createView(): View = NowPlayingView().also { npView = it }

    override fun onRotate(steps: Int) {
        val now = SystemClock.uptimeMillis()
        modeTouchedAt = now
        when (mode) {
            Mode.NORMAL -> {
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (cur + steps).coerceIn(0, max), 0)
                volumeUntil = now + 1800
            }
            Mode.SCRUB -> {
                val dur = player.duration()
                if (dur <= 0) return
                val step = (dur / 80).coerceIn(2_000, 10_000)
                val base = if (scrubTarget >= 0) scrubTarget else player.position()
                scrubTarget = (base + steps * step).coerceIn(0, dur - 500)
                scrubCommitAt = now + 350
                npView.postDelayed({ commitScrub() }, 360)
            }
            Mode.SHUFFLE -> player.setShuffle(!player.state.value.shuffle)
            Mode.REPEAT -> player.setRepeat(((player.state.value.repeatMode + if (steps > 0) 1 else 2) % 3))
        }
        npView.invalidate()
    }

    private fun commitScrub() {
        if (scrubTarget < 0 || SystemClock.uptimeMillis() < scrubCommitAt) return
        player.seekTo(scrubTarget)
        scrubTarget = -1
        npView.invalidate()
    }

    override fun onSelect() {
        if (!player.state.value.hasQueue) return
        mode = Mode.entries[(mode.ordinal + 1) % Mode.entries.size]
        modeTouchedAt = SystemClock.uptimeMillis()
        if (mode != Mode.SCRUB) { commitScrub(); scrubTarget = -1 }
        npView.invalidate()
    }

    override fun onSelectLong() {
        val t = player.currentTrack() ?: return
        ui.push(Menus.trackOptions(ui, t, fromNowPlaying = true))
    }

    override fun onBack(): Boolean {
        if (mode == Mode.NORMAL) return false
        mode = Mode.NORMAL
        scrubTarget = -1
        npView.invalidate()
        return true
    }

    override fun onPlayerChanged() { if (::npView.isInitialized) npView.invalidate() }
    override fun onLibraryChanged() { if (::npView.isInitialized) npView.invalidate() }

    @SuppressLint("ViewConstructor")
    private inner class NowPlayingView : View(ui.activity) {
        private val dp = ui.dp
        private val small = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        private val body = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val img = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val fade = Paint()
        private val path = Path()
        private val rect = RectF()
        private val dst = Rect()
        private val artListener: () -> Unit = { invalidate() }
        private val ticker = object : Runnable {
            override fun run() {
                if (mode != Mode.NORMAL && SystemClock.uptimeMillis() - modeTouchedAt > 6000) {
                    mode = Mode.NORMAL
                    scrubTarget = -1
                }
                invalidate()
                postDelayed(this, if (ui.liteMode) 1000 else 500)
            }
        }
        private var progressTop = 0f

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            ui.app.artwork.addListener(artListener)
            post(ticker)
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(ticker)
            ui.app.artwork.removeListener(artListener)
            super.onDetachedFromWindow()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_DOWN) return true
            if (e.action != MotionEvent.ACTION_UP) return true
            if (e.y >= progressTop - 16 * dp) {
                val dur = player.duration()
                if (dur > 0) {
                    val pad = 14 * dp
                    val f = ((e.x - pad) / (width - 2 * pad)).coerceIn(0f, 1f)
                    player.seekTo((dur * f).toLong())
                }
            } else {
                player.togglePlay()
            }
            invalidate()
            return true
        }

        override fun onDraw(c: Canvas) {
            val p = ui.palette
            val w = width.toFloat()
            val h = height.toFloat()
            c.drawColor(p.bg)
            val st = player.state.value
            val track = player.currentTrack()
            if (track == null || !st.hasQueue) {
                drawEmpty(c, w, h)
                return
            }
            val pad = 14 * dp

            // "3 of 12"
            small.color = p.secondary
            small.textSize = h * 0.058f
            small.textAlign = Paint.Align.CENTER
            c.drawText("${st.index + 1} of ${st.count}", w / 2, pad + small.textSize * 0.8f, small)
            small.textAlign = Paint.Align.LEFT

            // Artwork with reflection
            val art = min(w * 0.44f, h * 0.50f)
            val artTop = pad + small.textSize * 1.6f
            val bmp = ui.app.artwork.album(track.albumId, track, if (ui.liteMode) ArtworkLoader.Quality.LITE_LARGE else ArtworkLoader.Quality.LARGE)
            drawArt(c, bmp, pad, artTop, art)

            // Text block
            val tx = pad + art + pad
            val tw = w - tx - pad
            titleP.color = p.text
            titleP.textSize = h * 0.075f
            body.color = p.secondary
            body.textSize = h * 0.062f
            var y = artTop + art * 0.18f
            val loved = ui.app.library.isLoved(track.id)
            val titleLines = wrap(track.title, titleP, tw, 2)
            for ((i, line) in titleLines.withIndex()) {
                var x = tx
                if (i == 0 && loved) { drawHeart(c, x + titleP.textSize * 0.45f, y - titleP.textSize * 0.35f, titleP.textSize * 0.42f); x += titleP.textSize * 1.05f }
                c.drawText(fit(line, titleP, tw - (x - tx)), x, y, titleP)
                y += titleP.textSize * 1.2f
            }
            y += body.textSize * 0.3f
            body.color = p.text
            c.drawText(fit(track.artist, body, tw), tx, y, body)
            y += body.textSize * 1.3f
            body.color = p.secondary
            c.drawText(fit(track.album, body, tw), tx, y, body)
            y += body.textSize * 1.6f
            // Shuffle / repeat badges
            small.textSize = h * 0.05f
            val badges = buildList {
                if (st.shuffle) add("Shuffle")
                when (st.repeatMode) { 1 -> add("Repeat One"); 2 -> add("Repeat All") }
                if (player.sleepTimer.active) add("Sleep: ${player.sleepTimer.label()}")
            }
            if (badges.isNotEmpty() && y < artTop + art) c.drawText(fit(badges.joinToString(" · "), small, tw), tx, y, small)

            // Bottom area: progress / scrub / volume / shuffle / repeat
            val barY = h - pad - h * 0.13f
            progressTop = barY
            val now = SystemClock.uptimeMillis()
            when {
                mode == Mode.SHUFFLE -> drawChoice(c, barY, w, pad, "Shuffle", listOf("Off", "Songs"), if (st.shuffle) 1 else 0)
                mode == Mode.REPEAT -> drawChoice(c, barY, w, pad, "Repeat", listOf("Off", "One", "All"), st.repeatMode)
                mode == Mode.NORMAL && now < volumeUntil -> drawVolume(c, barY, w, pad)
                else -> drawProgress(c, barY, w, pad, st.durationMs, scrubbing = mode == Mode.SCRUB)
            }
        }

        private fun drawArt(c: Canvas, bmp: Bitmap?, x: Float, y: Float, s: Float) {
            val p = ui.palette
            dst.set(x.toInt(), y.toInt(), (x + s).toInt(), (y + s).toInt())
            if (bmp != null) c.drawBitmap(bmp, null, dst, img) else {
                paint.shader = LinearGradient(0f, y, 0f, y + s, p.placeholderTop, p.placeholderBottom, Shader.TileMode.CLAMP)
                c.drawRect(dst, paint)
                paint.shader = null
                paint.color = 0xAAFFFFFF.toInt()
                val cx = x + s * 0.45f; val cy = y + s * 0.64f
                c.drawCircle(cx, cy, s * 0.1f, paint)
                c.drawRect(cx + s * 0.07f, y + s * 0.26f, cx + s * 0.12f, cy, paint)
                c.drawRect(cx + s * 0.07f, y + s * 0.26f, cx + s * 0.27f, y + s * 0.33f, paint)
            }
            if (ui.liteMode) return
            // Reflection
            val rh = s * 0.22f
            c.save()
            c.clipRect(x, y + s, x + s, y + s + rh)
            c.translate(0f, 2 * (y + s))
            c.scale(1f, -1f)
            if (bmp != null) c.drawBitmap(bmp, null, dst, img)
            else {
                paint.shader = LinearGradient(0f, y, 0f, y + s, p.placeholderTop, p.placeholderBottom, Shader.TileMode.CLAMP)
                c.drawRect(dst, paint)
                paint.shader = null
            }
            c.restore()
            fade.shader = LinearGradient(0f, y + s, 0f, y + s + rh, (p.bg and 0x00FFFFFF) or 0x99000000.toInt(), p.bg, Shader.TileMode.CLAMP)
            c.drawRect(x, y + s, x + s, y + s + rh + 1, fade)
        }

        private fun drawProgress(c: Canvas, y: Float, w: Float, pad: Float, dur: Long, scrubbing: Boolean) {
            val p = ui.palette
            val pos = if (scrubbing && scrubTarget >= 0) scrubTarget else player.position()
            val bh = height * 0.045f
            rect.set(pad, y, w - pad, y + bh)
            paint.shader = null
            paint.color = p.trackBg
            c.drawRect(rect, paint)
            val f = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
            if (scrubbing) {
                // Diamond knob, like the original scrubber
                paint.color = p.text
                val kx = pad + (w - 2 * pad) * f
                val ky = y + bh / 2
                val k = bh * 0.9f
                path.reset()
                path.moveTo(kx, ky - k); path.lineTo(kx + k * 0.7f, ky); path.lineTo(kx, ky + k); path.lineTo(kx - k * 0.7f, ky); path.close()
                c.drawPath(path, paint)
            } else {
                paint.shader = LinearGradient(0f, y, 0f, y + bh, p.fillTop, p.fillBottom, Shader.TileMode.CLAMP)
                c.drawRect(pad, y, pad + (w - 2 * pad) * f, y + bh, paint)
                paint.shader = null
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp * 0.8f
            paint.color = p.secondary
            c.drawRect(rect, paint)
            paint.style = Paint.Style.FILL
            small.color = p.text
            small.textSize = height * 0.06f
            val ty = y + bh + small.textSize * 1.35f
            c.drawText(Text.duration(pos), pad, ty, small)
            val rem = "-" + Text.duration(max(0, dur - pos))
            c.drawText(rem, w - pad - small.measureText(rem), ty, small)
            if (scrubbing) {
                small.textAlign = Paint.Align.CENTER
                small.color = p.secondary
                c.drawText("Scrub", w / 2, ty, small)
                small.textAlign = Paint.Align.LEFT
            }
        }

        private fun drawVolume(c: Canvas, y: Float, w: Float, pad: Float) {
            val p = ui.palette
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val bh = height * 0.045f
            val icon = bh * 1.4f
            val left = pad + icon * 1.6f
            val right = w - pad - icon * 2.0f
            drawSpeaker(c, pad, y + bh / 2, icon, waves = 0)
            drawSpeaker(c, w - pad - icon * 1.6f, y + bh / 2, icon, waves = 2)
            rect.set(left, y, right, y + bh)
            paint.shader = null
            paint.color = p.trackBg
            c.drawRect(rect, paint)
            paint.shader = LinearGradient(0f, y, 0f, y + bh, p.fillTop, p.fillBottom, Shader.TileMode.CLAMP)
            c.drawRect(left, y, left + (right - left) * cur / max, y + bh, paint)
            paint.shader = null
            small.color = p.secondary
            small.textSize = height * 0.06f
            small.textAlign = Paint.Align.CENTER
            c.drawText("Volume", w / 2, y + bh + small.textSize * 1.35f, small)
            small.textAlign = Paint.Align.LEFT
        }

        private fun drawSpeaker(c: Canvas, x: Float, cy: Float, s: Float, waves: Int) {
            paint.color = ui.palette.text
            paint.style = Paint.Style.FILL
            c.drawRect(x, cy - s * 0.18f, x + s * 0.3f, cy + s * 0.18f, paint)
            path.reset()
            path.moveTo(x + s * 0.3f, cy - s * 0.18f); path.lineTo(x + s * 0.65f, cy - s * 0.45f)
            path.lineTo(x + s * 0.65f, cy + s * 0.45f); path.lineTo(x + s * 0.3f, cy + s * 0.18f); path.close()
            c.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s * 0.08f
            for (i in 1..waves) {
                val r = s * (0.2f + 0.2f * i)
                rect.set(x + s * 0.65f - r, cy - r, x + s * 0.65f + r, cy + r)
                c.drawArc(rect, -45f, 90f, false, paint)
            }
            paint.style = Paint.Style.FILL
        }

        private fun drawChoice(c: Canvas, y: Float, w: Float, pad: Float, label: String, options: List<String>, sel: Int) {
            val p = ui.palette
            val h = height * 0.09f
            val segW = (w - 2 * pad) / options.size
            small.textSize = height * 0.058f
            small.textAlign = Paint.Align.CENTER
            for ((i, o) in options.withIndex()) {
                rect.set(pad + i * segW, y - h * 0.4f, pad + (i + 1) * segW, y + h * 0.6f)
                if (i == sel) {
                    paint.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, p.selTop, p.selBottom, Shader.TileMode.CLAMP)
                    c.drawRoundRect(rect, 6 * dp, 6 * dp, paint)
                    paint.shader = null
                    small.color = p.selText
                } else {
                    paint.color = p.trackBg
                    c.drawRoundRect(rect, 6 * dp, 6 * dp, paint)
                    small.color = p.text
                }
                c.drawText(o, rect.centerX(), rect.centerY() - (small.ascent() + small.descent()) / 2, small)
            }
            small.color = p.secondary
            c.drawText(label, w / 2, y + h * 0.6f + small.textSize * 1.4f, small)
            small.textAlign = Paint.Align.LEFT
        }

        private fun drawHeart(c: Canvas, cx: Float, cy: Float, r: Float) {
            paint.color = 0xFFE5484D.toInt()
            path.reset()
            path.moveTo(cx, cy + r)
            path.cubicTo(cx - r * 1.6f, cy - r * 0.1f, cx - r * 0.7f, cy - r * 1.3f, cx, cy - r * 0.45f)
            path.cubicTo(cx + r * 0.7f, cy - r * 1.3f, cx + r * 1.6f, cy - r * 0.1f, cx, cy + r)
            c.drawPath(path, paint)
        }

        private fun drawEmpty(c: Canvas, w: Float, h: Float) {
            val p = ui.palette
            titleP.color = p.text
            titleP.textSize = h * 0.08f
            titleP.textAlign = Paint.Align.CENTER
            c.drawText("Nothing Playing", w / 2, h * 0.45f, titleP)
            titleP.textAlign = Paint.Align.LEFT
            small.color = p.secondary
            small.textSize = h * 0.058f
            small.textAlign = Paint.Align.CENTER
            c.drawText("Press ▶❚❚ to shuffle your music", w / 2, h * 0.58f, small)
            small.textAlign = Paint.Align.LEFT
        }

        private fun fit(s: String, p: TextPaint, w: Float) = TextUtils.ellipsize(s, p, max(0f, w), TextUtils.TruncateAt.END).toString()

        private fun wrap(s: String, p: TextPaint, w: Float, maxLines: Int): List<String> {
            if (p.measureText(s) <= w) return listOf(s)
            val words = s.split(' ')
            val lines = ArrayList<String>()
            var cur = ""
            for (word in words) {
                val next = if (cur.isEmpty()) word else "$cur $word"
                if (p.measureText(next) <= w || cur.isEmpty()) cur = next else {
                    lines += cur
                    cur = word
                    if (lines.size == maxLines - 1) break
                }
            }
            val usedWords = lines.sumOf { it.split(' ').size }
            val rest = words.drop(usedWords).joinToString(" ")
            if (rest.isNotEmpty()) lines += rest
            return lines.take(maxLines)
        }
    }
}
