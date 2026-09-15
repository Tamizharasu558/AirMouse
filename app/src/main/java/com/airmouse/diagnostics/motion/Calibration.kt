package com.airmouse.diagnostics.motion

import kotlin.math.sqrt

/**
 * Collects stationary samples and produces a gyroscope bias vector to subtract from
 * future readings. Rejects the batch if variance says the phone was not still.
 */
class CalibrationCollector(
    private val minSamples: Int = 40,
    private val maxStdDevRadS: Float = 0.02f
) {
    private val xs = mutableListOf<Float>()
    private val ys = mutableListOf<Float>()
    private val zs = mutableListOf<Float>()

    fun add(x: Float, y: Float, z: Float) {
        xs.add(x); ys.add(y); zs.add(z)
    }

    val hasEnoughSamples: Boolean get() = xs.size >= minSamples

    /** Mean bias, or null if not enough samples / too much movement during capture. */
    fun compute(): Vector3? {
        if (!hasEnoughSamples) return null
        val bx = xs.average().toFloat()
        val by = ys.average().toFloat()
        val bz = zs.average().toFloat()
        val worst = maxOf(stdDev(xs, bx), stdDev(ys, by), stdDev(zs, bz))
        return if (worst <= maxStdDevRadS) Vector3(bx, by, bz) else null
    }

    fun reset() { xs.clear(); ys.clear(); zs.clear() }

    private fun stdDev(values: List<Float>, mean: Float): Float =
        sqrt(values.map { (it - mean) * (it - mean) }.average().toFloat())
}
