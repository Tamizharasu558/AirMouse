package com.airmouse.diagnostics.motion

/** Pure Kotlin motion model — no Android dependencies, unit-testable off-device. */

data class Vector3(val x: Float, val y: Float, val z: Float) {
    companion object { val ZERO = Vector3(0f, 0f, 0f) }
}

data class MotionOutput(
    val dx: Int,
    val dy: Int,
    val filteredVx: Float,
    val filteredVy: Float
)

/**
 * Tunables for the air-mouse pipeline.
 *
 * Axis mapping (default, phone held portrait pointing at the screen like a wand):
 *  - cursor X  ← rotation around the phone's Y axis (pan left/right), inverted so a
 *                clockwise-from-above rotation moves the cursor right;
 *  - cursor Y  ← rotation around the phone's X axis (tilt up/down), +X rotation → cursor down
 *                (HID convention: +Y movement is down).
 */
data class MotionConfig(
    val sensitivity: Float = 800f,      // HID counts per radian, before acceleration
    val deadZoneRadS: Float = 0.005f,   // per-axis angular-velocity threshold
    val emaAlpha: Float = 0.35f,        // 0..1; higher = snappier, noisier
    val acceleration: Float = 0.35f,    // 0 = off; extra gain for fast motions
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val maxDelta: Int = 127             // HID int8 per-report limit
)
