package com.airmouse.diagnostics.hid

/**
 * HID boot-mouse input report encoder (buttons + relative X/Y + wheel).
 * Pure Kotlin; reusable as the payload for BluetoothHidDevice.sendReport in Phase 4.
 *
 * Layout: byte0 buttons bitfield, byte1 X (int8), byte2 Y (int8), byte3 wheel (int8).
 */
object MouseReport {
    const val BUTTON_NONE = 0
    const val BUTTON_LEFT = 1
    const val BUTTON_RIGHT = 2
    const val BUTTON_MIDDLE = 4

    fun buttonsByte(left: Boolean = false, right: Boolean = false, middle: Boolean = false): Int {
        var b = 0
        if (left) b = b or BUTTON_LEFT
        if (right) b = b or BUTTON_RIGHT
        if (middle) b = b or BUTTON_MIDDLE
        return b
    }

    fun clampRel(value: Int): Int = value.coerceIn(-127, 127)

    fun encode(buttons: Int, dx: Int, dy: Int, wheel: Int = 0): ByteArray = byteArrayOf(
        buttons.toByte(),
        clampRel(dx).toByte(),
        clampRel(dy).toByte(),
        clampRel(wheel).toByte()
    )

    fun move(dx: Int, dy: Int): ByteArray = encode(BUTTON_NONE, dx, dy)
    fun press(buttons: Int): ByteArray = encode(buttons, 0, 0)
    fun release(): ByteArray = encode(BUTTON_NONE, 0, 0)
    fun wheel(amount: Int): ByteArray = encode(BUTTON_NONE, 0, 0, amount)
}
