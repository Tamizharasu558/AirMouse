package com.airmouse.diagnostics.motion

/** Exponential moving average — the beginner-friendly low-pass filter for the first version. */
class EmaFilter(private val alpha: Float) {
    private var value = 0f
    private var primed = false

    fun apply(sample: Float): Float {
        if (!primed) {
            value = sample
            primed = true
        } else {
            value += alpha * (sample - value)
        }
        return value
    }

    fun reset() { value = 0f; primed = false }
}
