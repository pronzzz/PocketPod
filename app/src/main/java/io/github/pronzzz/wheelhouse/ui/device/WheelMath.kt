package io.github.pronzzz.wheelhouse.ui.device

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

enum class WheelButton { MENU, PREV, NEXT, PLAY, SELECT }

/** Pure geometry for the click wheel, kept free of Android types so it can be unit tested. */
object WheelMath {

    /** Angle of a point relative to the wheel centre, in radians. 0 = right, grows clockwise (screen y points down). */
    fun angle(dx: Float, dy: Float): Double = atan2(dy.toDouble(), dx.toDouble())

    /** Smallest signed difference between two angles, in (-PI, PI]. Positive = clockwise. */
    fun delta(from: Double, to: Double): Double {
        var d = to - from
        while (d > PI) d -= 2 * PI
        while (d <= -PI) d += 2 * PI
        return d
    }

    /** Which button a tap at (dx, dy) from the centre hits, or null if it's outside the wheel. */
    fun buttonAt(dx: Float, dy: Float, outerR: Float, centerR: Float): WheelButton? {
        val r = hypot(dx, dy)
        if (r > outerR) return null
        if (r <= centerR) return WheelButton.SELECT
        val deg = Math.toDegrees(angle(dx, dy))
        return when {
            deg >= -135 && deg < -45 -> WheelButton.MENU
            deg >= -45 && deg < 45 -> WheelButton.NEXT
            deg >= 45 && deg < 135 -> WheelButton.PLAY
            else -> WheelButton.PREV
        }
    }

    /** Degrees of rotation per list step for each sensitivity setting (low, medium, high). */
    fun stepDegrees(sensitivity: Int) = when (sensitivity) {
        0 -> 30.0
        2 -> 13.0
        else -> 20.0
    }
}

/** Turns a stream of small angle changes into whole "clicks", keeping the remainder. */
class RotationAccumulator(private val stepRadians: Double) {
    private var acc = 0.0

    fun feed(deltaRadians: Double): Int {
        acc += deltaRadians
        var steps = 0
        while (acc >= stepRadians) { acc -= stepRadians; steps++ }
        while (acc <= -stepRadians) { acc += stepRadians; steps-- }
        return steps
    }

    fun reset() { acc = 0.0 }
}
