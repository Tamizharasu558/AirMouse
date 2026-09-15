package com.airmouse.diagnostics.diagnostics

/** Pure decision logic for capability checks — no Android dependencies, fully unit-testable. */
object CapabilityChecks {
    fun gyro(present: Boolean) = Check(
        "gyroscope", "Gyroscope",
        if (present) CheckStatus.PASS else CheckStatus.FAIL,
        if (present) "Primary motion sensor for air-mouse movement." else "Missing — air-mouse mode will not be possible."
    )

    fun accel(present: Boolean) = Check(
        "accelerometer", "Accelerometer",
        if (present) CheckStatus.PASS else CheckStatus.FAIL,
        if (present) "Stabilises motion estimation." else "Missing — motion fusion will be limited."
    )

    fun gravity(present: Boolean) = Check(
        "gravity", "Gravity",
        if (present) CheckStatus.PASS else CheckStatus.WARN,
        if (present) "Reference for tilt compensation." else "Missing — tilt handling must use accelerometer instead."
    )

    fun magnetometer(present: Boolean) = Check(
        "magnetometer", "Magnetometer",
        if (present) CheckStatus.PASS else CheckStatus.WARN,
        if (present) "Enables absolute-orientation (yaw) stabilisation." else "Missing — yaw drift cannot be corrected automatically."
    )

    fun bluetooth(hasAdapter: Boolean, enabled: Boolean) = Check(
        "bluetooth", "Bluetooth adapter",
        when {
            !hasAdapter -> CheckStatus.FAIL
            enabled -> CheckStatus.PASS
            else -> CheckStatus.WARN
        },
        when {
            !hasAdapter -> "No Bluetooth adapter — HID mode impossible."
            enabled -> "Adapter present and enabled."
            else -> "Adapter present but disabled; enable Bluetooth in system settings."
        }
    )

    fun ir(hasEmitter: Boolean, serviceAvailable: Boolean) = Check(
        "ir", "IR transmitter",
        when {
            hasEmitter -> CheckStatus.PASS
            !serviceAvailable -> CheckStatus.UNKNOWN
            else -> CheckStatus.WARN
        },
        when {
            hasEmitter -> "ConsumerIrManager reports an emitter; IR remote mode possible."
            !serviceAvailable -> "Consumer IR service not available on this device."
            else -> "No IR emitter (common on many recent Redmi models)."
        }
    )

    fun hidSupport(sdkInt: Int, hasBluetooth: Boolean): Pair<Boolean, String?> = when {
        sdkInt < 28 -> false to "BluetoothHidDevice requires Android 9 (API 28); this device runs API $sdkInt."
        !hasBluetooth -> false to "No Bluetooth adapter on this device."
        else -> true to null
    }
}
