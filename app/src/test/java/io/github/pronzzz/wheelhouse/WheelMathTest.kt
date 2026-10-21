package io.github.pronzzz.wheelhouse

import io.github.pronzzz.wheelhouse.ui.device.RotationAccumulator
import io.github.pronzzz.wheelhouse.ui.device.WheelButton
import io.github.pronzzz.wheelhouse.ui.device.WheelMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI

class WheelMathTest {

    private val outer = 100f
    private val center = 37f

    @Test fun tapZonesMapToButtons() {
        assertEquals(WheelButton.MENU, WheelMath.buttonAt(0f, -80f, outer, center))
        assertEquals(WheelButton.PLAY, WheelMath.buttonAt(0f, 80f, outer, center))
        assertEquals(WheelButton.NEXT, WheelMath.buttonAt(80f, 0f, outer, center))
        assertEquals(WheelButton.PREV, WheelMath.buttonAt(-80f, 0f, outer, center))
        assertEquals(WheelButton.SELECT, WheelMath.buttonAt(10f, 10f, outer, center))
    }

    @Test fun tapsOutsideTheWheelAreIgnored() {
        assertNull(WheelMath.buttonAt(90f, 90f, outer, center))
    }

    @Test fun deltaWrapsAroundTheSeam() {
        // From just below +PI to just above -PI is a small clockwise move, not a full turn back.
        val d = WheelMath.delta(PI - 0.1, -PI + 0.1)
        assertEquals(0.2, d, 1e-9)
        assertEquals(-0.2, WheelMath.delta(-PI + 0.1, PI - 0.1), 1e-9)
    }

    @Test fun clockwiseIsPositive() {
        // Screen coordinates: y grows downwards, so moving from "right" to "bottom" is clockwise.
        val a = WheelMath.angle(1f, 0f)
        val b = WheelMath.angle(0f, 1f)
        assert(WheelMath.delta(a, b) > 0)
    }

    @Test fun accumulatorEmitsWholeStepsAndKeepsRemainder() {
        val step = Math.toRadians(20.0)
        val acc = RotationAccumulator(step)
        assertEquals(0, acc.feed(Math.toRadians(15.0)))
        assertEquals(1, acc.feed(Math.toRadians(10.0)))   // 25° total -> 1 step, 5° left
        assertEquals(2, acc.feed(Math.toRadians(36.0)))   // 41° -> 2 steps
        assertEquals(-1, acc.feed(Math.toRadians(-22.0))) // 1° - 22° = -21° -> -1 step
    }

    @Test fun sensitivityStepsGetSmallerWhenFaster() {
        assert(WheelMath.stepDegrees(0) > WheelMath.stepDegrees(1))
        assert(WheelMath.stepDegrees(1) > WheelMath.stepDegrees(2))
    }
}
