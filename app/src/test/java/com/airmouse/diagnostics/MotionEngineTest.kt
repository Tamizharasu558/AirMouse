package com.airmouse.diagnostics

import com.airmouse.diagnostics.hid.MouseReport
import com.airmouse.diagnostics.motion.CalibrationCollector
import com.airmouse.diagnostics.motion.EmaFilter
import com.airmouse.diagnostics.motion.MotionConfig
import com.airmouse.diagnostics.motion.MotionProcessor
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MotionGainTest {
    private val t0 = 1_000_000_000L

    private fun feed(proc: MotionProcessor, gy: Float, durationS: Float): Int {
        var dx = 0
        val steps = (durationS * 100).toInt()
        repeat(steps) { i ->
            dx += proc.onGyro(t0 + i * 10_000_000L, 0f, gy, 0f).dx
        }
        return dx
    }

    @Test
    fun `acceleration increases gain at high angular speed`() {
        val slow = MotionProcessor(MotionConfig(sensitivity = 3000f, acceleration = 0f, emaAlpha = 1f))
        val fast = MotionProcessor(MotionConfig(sensitivity = 3000f, acceleration = 1f, emaAlpha = 1f))
        val s = abs(feed(slow, 3f, 0.5f))
        val f = abs(feed(fast, 3f, 0.5f))
        assertTrue(f > s)
    }

    @Test
    fun `tilt around X drives cursor Y downward`() {
        val proc = MotionProcessor(MotionConfig(sensitivity = 1000f, acceleration = 0f, emaAlpha = 1f))
        var dy = 0
        repeat(100) { i -> dy += proc.onGyro(t0 + i * 10_000_000L, 1f, 0f, 0f).dy }
        assertEquals(990, dy)            // dy ← +vx
    }

    @Test
    fun `invert flags flip output signs`() {
        val normal = MotionProcessor(MotionConfig(emaAlpha = 1f, acceleration = 0f))
        val inverted = MotionProcessor(MotionConfig(emaAlpha = 1f, acceleration = 0f, invertX = true, invertY = true))
        val n = feed(normal, 1f, 0.2f)
        val i = feed(inverted, 1f, 0.2f)
        assertTrue(n < 0 && i > 0)
    }

    @Test
    fun `report deltas never exceed int8 HID range`() {
        val proc = MotionProcessor(MotionConfig(sensitivity = 100000f, emaAlpha = 1f))
        var worst = 0
        repeat(50) { i ->
            val out = proc.onGyro(t0 + i * 10_000_000L, 0f, 50f, 0f)
            worst = maxOf(worst, abs(out.dx), abs(out.dy))
        }
        assertTrue(worst <= 127)
    }
}

class EmaFilterTest {
    @Test
    fun `primes on first sample then blends with alpha`() {
        val f = EmaFilter(0.35f)
        assertEquals(1f, f.apply(1f), 1e-6f)
        assertEquals(0.65f, f.apply(0f), 1e-6f)
        assertEquals(0.4225f, f.apply(0f), 1e-6f)
    }

    @Test
    fun `reset returns to priming behaviour`() {
        val f = EmaFilter(0.5f)
        f.apply(10f); f.apply(10f)
        f.reset()
        assertEquals(2f, f.apply(2f), 1e-6f)
    }
}

class CalibrationCollectorTest {
    @Test
    fun `rejects capture with too much movement`() {
        val c = CalibrationCollector(minSamples = 10, maxStdDevRadS = 0.02f)
        repeat(10) { i -> c.add(if (i % 2 == 0) 0f else 0.5f, 0f, 0f) }
        assertNull(c.compute())
    }

    @Test
    fun `returns mean bias for a still capture`() {
        val c = CalibrationCollector(minSamples = 10, maxStdDevRadS = 0.02f)
        repeat(10) { _ -> c.add(0.012f, -0.017f, 0.005f) }
        val bias = c.compute()
        assertNotNull(bias)
        assertEquals(0.012f, bias!!.x, 1e-6f)
        assertEquals(-0.017f, bias.y, 1e-6f)
        assertEquals(0.005f, bias.z, 1e-6f)
    }

    @Test
    fun `needs the minimum sample count`() {
        val c = CalibrationCollector(minSamples = 10)
        repeat(9) { _ -> c.add(0f, 0f, 0f) }
        assertNull(c.compute())
        c.add(0f, 0f, 0f)
        assertNotNull(c.compute())
    }
}

class MouseReportTest {
    @Test
    fun `encodes boot mouse report with clamped int8 deltas`() {
        val bytes = MouseReport.encode(MouseReport.buttonsByte(left = true), 130, -200)
        assertArrayEquals(byteArrayOf(0x01, 0x7F, 0x81.toByte(), 0x00), bytes)
    }

    @Test
    fun `button bits combine`() {
        assertEquals(3, MouseReport.buttonsByte(left = true, right = true))
        assertEquals(4, MouseReport.buttonsByte(middle = true))
        assertEquals(0, MouseReport.buttonsByte())
    }
}
