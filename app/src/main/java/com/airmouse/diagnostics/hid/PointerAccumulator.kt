package com.airmouse.diagnostics.hid

/**
 * Converts touchpad finger deltas (dp) into integer HID counts, retaining the
 * fractional remainder so slow drags are not lost. Pure Kotlin; unit-testable.
 */
class PointerAccumulator(
    private val countsPerDp: Float = 2.5f,
    private val maxDelta: Int = 127
) {
    private var carryX = 0f
    private var carryY = 0f

    /** Returns the integer chunk to send now, or null when less than 1 count accumulated. */
    fun add(dx: Float, dy: Float): Pair<Int, Int>? {
        carryX += dx * countsPerDp
        carryY += dy * countsPerDp
        val ix = carryX.toInt()
        val iy = carryY.toInt()
        if (ix == 0 && iy == 0) return null
        carryX -= ix
        carryY -= iy
        return ix.coerceIn(-maxDelta, maxDelta) to iy.coerceIn(-maxDelta, maxDelta)
    }

    fun reset() {
        carryX = 0f
        carryY = 0f
    }
}