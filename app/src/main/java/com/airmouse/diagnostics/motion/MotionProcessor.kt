package com.airmouse.diagnostics.motion

import kotlin.math.abs
import kotlin.math.max

/**
 * Gyroscope → relative cursor movement (dx, dy in HID counts).
 *
 * Pipeline: bias subtraction → EMA smoothing → dead zone → axis mapping →
 * pointer acceleration → time integration → integer emission with fractional carry.
 */
class MotionProcessor(private val config: MotionConfig = MotionConfig()) {
    private var bias = Vector3.ZERO
    private val filterX = EmaFilter(config.emaAlpha)
    private val filterY = EmaFilter(config.emaAlpha)
    private var lastTsNs: Long? = null
    private var carryX = 0f
    private var carryY = 0f

    fun setBias(value: Vector3) { bias = value }
    fun reset() {
        filterX.reset(); filterY.reset()
        lastTsNs = null; carryX = 0f; carryY = 0f
    }

    fun onGyro(tsNs: Long, gx: Float, gy: Float, gz: Float): MotionOutput {
        val last = lastTsNs
        lastTsNs = tsNs
        // dt in seconds; clamp to guard against stalls/resume spikes and ignore the first sample.
        val dt = if (last == null) 0f
                 else ((tsNs - last).coerceAtLeast(0) / 1_000_000_000f).coerceAtMost(0.1f)

        val rawX = gx - bias.x
        val rawY = gy - bias.y

        val fx = filterX.apply(rawX)
        val fy = filterY.apply(rawY)

        val vx = if (abs(fx) < config.deadZoneRadS) 0f else fx
        val vy = if (abs(fy) < config.deadZoneRadS) 0f else fy

        // Pointer acceleration: extra gain scaled by normalised angular speed (≤ 1 extra × (1+a)).
        val speed = max(abs(vx), abs(vy))
        val gain = 1f + config.acceleration * (speed / 3f).coerceIn(0f, 1f)

        // Mapping (see MotionConfig): dx ∝ −vy, dy ∝ +vx.
        val signX = if (config.invertX) 1f else -1f
        val signY = if (config.invertY) -1f else 1f
        carryX += signX * vy * dt * config.sensitivity * gain
        carryY += signY * vx * dt * config.sensitivity * gain

        val dx = clamp(carryX.toInt())
        val dy = clamp(carryY.toInt())
        carryX -= dx
        carryY -= dy

        return MotionOutput(dx, dy, vx, vy)
    }

    private fun clamp(value: Int) = value.coerceIn(-config.maxDelta, config.maxDelta)
}
