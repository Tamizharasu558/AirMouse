package com.airmouse.diagnostics.ui

import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.diagnostics.hid.HidAppStatus
import com.airmouse.diagnostics.hid.HidMouseClient
import com.airmouse.diagnostics.hid.MouseReport
import com.airmouse.diagnostics.hid.PointerAccumulator
import com.airmouse.diagnostics.motion.CalibrationCollector
import com.airmouse.diagnostics.motion.MotionConfig
import com.airmouse.diagnostics.motion.MotionProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DeviceRow(
    val address: String,
    val name: String,
    val connectionState: Int
)

/** One sent report, for the on-screen log. */
data class ClickLogEntry(val text: String, val sent: Boolean)

enum class AirMode { OFF, AIR, CALIBRATING }

class MouseViewModel(app: Application) : AndroidViewModel(app) {

    private val adapter =
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val client = HidMouseClient(app, adapter)
    private val sensorManager =
        app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val motion = MotionProcessor(MotionConfig())
    private val calibration = CalibrationCollector()
    private val touchAccumulator = PointerAccumulator()
    private var gyroJob: Job? = null

    private val _status = MutableStateFlow(HidAppStatus.IDLE)
    val status: StateFlow<HidAppStatus> = _status.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceRow>>(emptyList())
    val devices: StateFlow<List<DeviceRow>> = _devices.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _clickLog = MutableStateFlow<List<ClickLogEntry>>(emptyList())
    val clickLog: StateFlow<List<ClickLogEntry>> = _clickLog.asStateFlow()

    private val _airMode = MutableStateFlow(AirMode.OFF)
    val airMode: StateFlow<AirMode> = _airMode.asStateFlow()

    private val _calProgress = MutableStateFlow(0f)
    val calProgress: StateFlow<Float> = _calProgress.asStateFlow()

    private val _sensitivity = MutableStateFlow(MotionConfig().sensitivity)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _connectedLabel = MutableStateFlow<String?>(null)
    val connectedLabel: StateFlow<String?> = _connectedLabel.asStateFlow()

    /** True while the left button is held down (HID button bit latched until release). */
    private val _lHeld = MutableStateFlow(false)
    val lHeld: StateFlow<Boolean> = _lHeld.asStateFlow()

    init {
        client.appStatus.onEach { _status.value = it }.launchIn(viewModelScope)
        client.lastError.onEach { if (it != null) _lastError.value = it }.launchIn(viewModelScope)
        client.connections.onEach { m ->
            refreshDeviceList(m)
            val connected = m.entries.firstOrNull { it.value == BluetoothProfile.STATE_CONNECTED }
            _connectedLabel.value = connected?.let { entry ->
                _devices.value.firstOrNull { it.address == entry.key }?.name ?: entry.key
            }
        }.launchIn(viewModelScope)
        client.start()
        refreshDeviceList()
    }

    /** True when a Bluetooth call was blocked by the missing BLUETOOTH_CONNECT runtime grant. */
    val securityFailure: StateFlow<Boolean> get() = client.securityFailure

    fun refreshDeviceList(states: Map<String, Int> = client.connections.value) {
        _devices.value = client.bondedDevices().map { d ->
            DeviceRow(
                address = d.address,
                name = runCatching { d.name }.getOrNull() ?: "(unnamed)",
                connectionState = states[d.address] ?: 0
            )
        }
    }

    fun connect(row: DeviceRow) {
        val device = runCatching { adapter?.getRemoteDevice(row.address) }.getOrNull() ?: return
        client.connect(device)
    }

    fun disconnect(row: DeviceRow) {
        val device = runCatching { adapter?.getRemoteDevice(row.address) }.getOrNull() ?: return
        client.disconnect(device)
    }

    fun sendPress(buttons: Int) = sendReport(MouseReport.press(buttons), "press b=$buttons")
    fun sendRelease() = sendReport(MouseReport.release(), "release")
    fun sendWheel(amount: Int) = sendReport(MouseReport.wheel(amount), "wheel $amount")

    /** Left button: tap to press and hold, tap again to release (for click-drag). */
    fun sendLPress() {
        sendReport(MouseReport.press(MouseReport.BUTTON_LEFT), "L press")
        _lHeld.value = true
    }

    fun sendLRelease() {
        sendReport(MouseReport.release(), "release")
        _lHeld.value = false
    }

    /** Right button: full click — press, brief gap so the host sees both edges, release. */
    fun rightClick() {
        viewModelScope.launch {
            sendReport(MouseReport.press(MouseReport.BUTTON_RIGHT), "R press")
            delay(60)
            sendReport(MouseReport.release(), "release")
        }
    }

    fun sendMove(dx: Int, dy: Int) = sendReport(MouseReport.move(dx, dy), "move $dx,$dy")

    private fun sendReport(report: ByteArray, label: String) {
        val ok = client.sendInput(report)
        _clickLog.value = (_clickLog.value + ClickLogEntry(label, ok)).takeLast(6)
    }

    /** Touchpad drag (dp) → integer HID counts via fractional accumulator. */
    fun onTouchDelta(dx: Float, dy: Float) {
        touchAccumulator.add(dx, dy)?.let { (ix, iy) -> sendMove(ix, iy) }
    }

    fun clearTouchAccumulator() = touchAccumulator.reset()

    fun setSensitivity(value: Float) {
        _sensitivity.value = value
        motion.reset()
    }

    fun startAirCalibration() {
        calibration.reset()
        _airMode.value = AirMode.CALIBRATING
        _calProgress.value = 0f
        collectGyro()
    }

    fun stopAir() {
        _airMode.value = AirMode.OFF
        motion.reset()
    }

    fun clearError() { _lastError.value = null }

    private fun collectGyro() {
        gyroJob?.cancel()
        gyroJob = viewModelScope.launch {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val v = event.values
                    when (_airMode.value) {
                        AirMode.CALIBRATING -> {
                            calibration.add(v[0], v[1], v[2])
                            if (calibration.hasEnoughSamples) {
                                val bias = calibration.compute()
                                if (bias != null) {
                                    motion.setBias(bias)
                                    _airMode.value = AirMode.AIR
                                } else {
                                    _lastError.value = "Phone moved during calibration — hold still"
                                    _airMode.value = AirMode.OFF
                                }
                            }
                        }
                        AirMode.AIR -> {
                            val out = motion.onGyro(event.timestamp, v[0], v[1], v[2])
                            if (out.dx != 0 || out.dy != 0) sendMove(out.dx, out.dy)
                        }
                        AirMode.OFF -> Unit
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            if (sensor == null) {
                _lastError.value = "No gyroscope"
                _airMode.value = AirMode.OFF
                return@launch
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            try {
                airMode.first { it == AirMode.OFF }
            } finally {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    override fun onCleared() {
        client.shutdown()
        super.onCleared()
    }
}
