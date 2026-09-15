package com.airmouse.diagnostics

import com.airmouse.diagnostics.motion.CalibrationCollector
import com.airmouse.diagnostics.motion.MotionConfig
import com.airmouse.diagnostics.motion.MotionProcessor
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MotionProcessorTest {
    private val t0 = 1_000_000_000L

    /** Feeds a constant gyro rate at 100 Hz and returns (totalDx, totalDy). */
    private fun feed(
        proc: MotionProcessor,
        gx: Float = 0f,
        gy: Float = 0f,
        durationS: Float,
        periodMs: Long = 10
    ): Pair<Int, Int> {
        var totalDx = 0
        var totalDy = 0
        val steps = (durationS * 1000 / periodMs).toInt()
        repeat(steps) { i ->
            val out = proc.onGyro(t0 + i * periodMs * 1_000_000L, gx, gy, 0f)
            totalDx += out.dx
            totalDy += out.dy
        }
        return totalDx to totalDy
    }

    @Test
    fun `no rotation produces no movement`() {
        val proc = MotionProcessor()
        val out = proc.onGyro(t0, 0f, 0f, 0f)
        assertEquals(0, out.dx)
        assertEquals(0, out.dy)
    }

    @Test
    fun `first sample has no dt so never jumps the cursor`() {
        val proc = MotionProcessor()
        val out = proc.onGyro(t0, 0f, 0.9f, 0f)
        assertEquals(0, out.dx + out.dy)
    }

    @Test
    fun `steady rotation integrates linearly over time`() {
        // sens=1000, 1 rad/s for 1 s at 100 Hz → 990 counts: first sample has dt=0
        val proc = MotionProcessor(MotionConfig(sensitivity = 1000f, acceleration = 0f, emaAlpha = 1f))
        val (dx, dy) = feed(proc, gy = 1f, durationS = 1f)
        assertEquals(-990, dx)   // dx ← −vy
        assertEquals(0, dy)
    }

    @Test
    fun `dead zone ignores sub-threshold drift`() {
        val cfg = MotionConfig(deadZoneRadS = 0.02f, emaAlpha = 1f, acceleration = 0f)
        val proc = MotionProcessor(cfg)
        proc.onGyro(t0, 0f, 0f, 0f)                       // prime EMA/timestamp
        val out = proc.onGyro(t0 + 10_000_000L, 0f, 0.01f, 0f)
        assertEquals(0, out.dx + out.dy)
    }

    @Test
    fun `bias subtraction removes constant offset`() {
        val proc = MotionProcessor(MotionConfig(emaAlpha = 1f, acceleration = 0f))
        proc.setBias(com.airmouse.diagnostics.motion.Vector3(0f, 0.9f, 0f))
        val (dx, dy) = feed(proc, gy = 0.9f, durationS = 1f)
        assertEquals(0, dx + dy)
    }
}
