package com.airmouse.diagnostics.diagnostics

import android.content.Context
import android.hardware.ConsumerIrManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.bluetooth.BluetoothManager

/** Wraps all Android capability lookups so the decision logic can be unit-tested without a device. */
class SensorScanner(private val context: Context) {

    fun scan(): CapabilityReport {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

        val gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val hasBluetooth = btManager?.adapter != null

        val checks = buildList {
            add(CapabilityChecks.gyro(gyroscope != null))
            add(CapabilityChecks.accel(accelerometer != null))
            add(CapabilityChecks.gravity(gravity != null))
            add(CapabilityChecks.magnetometer(magnetometer != null))
            add(CapabilityChecks.bluetooth(hasBluetooth, btManager?.adapter?.isEnabled == true))
            add(CapabilityChecks.ir(ir?.hasIrEmitter() == true, ir != null))
        }

        val sensorReports = mapOf(
            "gyroscope" to SensorReport.from(gyroscope),
            "accelerometer" to SensorReport.from(accelerometer),
            "gravity" to SensorReport.from(gravity),
            "magnetometer" to SensorReport.from(magnetometer)
        )

        val hidUnsupportedReason = CapabilityChecks.hidSupport(Build.VERSION.SDK_INT, hasBluetooth).second

        return CapabilityReport(checks, sensorReports, hidUnsupportedReason == null, hidUnsupportedReason)
    }

    fun gyroCheck(present: Boolean) = Check(
        "gyroscope", "Gyroscope",
        if (present) CheckStatus.PASS else CheckStatus.FAIL,
        if (present) "Primary motion sensor for air-mouse movement." else "Missing — air-mouse mode will not be possible."
    )

    fun accelCheck(present: Boolean) = Check(
        "accelerometer", "Accelerometer",
        if (present) CheckStatus.PASS else CheckStatus.FAIL,
        if (present) "Stabilises motion estimation." else "Missing — motion fusion will be limited."
    )

    fun gravityCheck(present: Boolean) = Check(
        "gravity", "Gravity",
        if (present) CheckStatus.PASS else CheckStatus.WARN,
        if (present) "Reference for tilt compensation." else "Missing — tilt handling must use accelerometer instead."
    )

    fun magCheck(present: Boolean) = Check(
        "magnetometer", "Magnetometer",
        if (present) CheckStatus.PASS else CheckStatus.WARN,
        if (present) "Enables absolute-orientation (yaw) stabilisation." else "Missing — yaw drift cannot be corrected automatically."
    )

    fun btCheck(hasAdapter: Boolean, enabled: Boolean) = Check(
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

    fun irCheck(hasEmitter: Boolean, serviceAvailable: Boolean) = Check(
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
}

data class SensorReport(
    val name: String,
    val vendor: String?,
    val maxRange: Float?,
    val resolution: Float?,
    val minDelayUs: Int?,
    val isDynamic: Boolean
) {
    companion object {
        fun from(sensor: Sensor?): SensorReport = if (sensor == null) SensorReport("", null, null, null, null, false)
        else SensorReport(sensor.name, sensor.vendor, sensor.maximumRange, sensor.resolution, sensor.minDelay, sensor.isDynamicSensor)
    }
}
