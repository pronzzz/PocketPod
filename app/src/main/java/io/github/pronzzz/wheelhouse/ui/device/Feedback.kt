package io.github.pronzzz.wheelhouse.ui.device

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.View
import io.github.pronzzz.wheelhouse.data.Prefs
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The wheel's tactile "click": a tiny synthesized tick plus a haptic pulse.
 * The sound is generated at first launch, so no third-party audio is bundled.
 */
class Feedback(context: Context, private val prefs: Prefs) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).build()

    private var tickId = 0
    private var pressId = 0

    init {
        val dir = context.cacheDir
        tickId = pool.load(writeWav(File(dir, "tick_v1.wav"), 3600.0, 0.0011, 0.35).path, 1)
        pressId = pool.load(writeWav(File(dir, "press_v1.wav"), 1700.0, 0.0024, 0.45).path, 1)
    }

    fun tick(view: View) {
        if (prefs.clickSound) pool.play(tickId, 0.55f, 0.55f, 1, 0, 1f)
        if (prefs.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }

    fun press(view: View) {
        if (prefs.clickSound) pool.play(pressId, 0.7f, 0.7f, 1, 0, 1f)
        if (prefs.haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }

    fun release() = pool.release()

    /** A short decaying sine + noise burst, written as 16-bit mono PCM WAV. */
    private fun writeWav(file: File, freq: Double, decay: Double, noise: Double): File {
        if (file.exists() && file.length() > 44) return file
        val rate = 44_100
        val n = (rate * 0.012).toInt()
        val rnd = Random(7)
        val pcm = ShortArray(n) { i ->
            val t = i.toDouble() / rate
            val env = exp(-t / decay)
            val v = env * ((1 - noise) * sin(2 * PI * freq * t) + noise * (rnd.nextDouble() * 2 - 1))
            (v * 0.9 * Short.MAX_VALUE).toInt().toShort()
        }
        val bb = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        bb.put("data".toByteArray()).putInt(n * 2)
        pcm.forEach { bb.putShort(it) }
        file.writeBytes(bb.array())
        return file
    }
}
