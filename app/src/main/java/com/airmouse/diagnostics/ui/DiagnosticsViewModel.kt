package com.airmouse.diagnostics.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.diagnostics.diagnostics.CapabilityReport
import com.airmouse.diagnostics.diagnostics.SensorScanner
import com.airmouse.diagnostics.live.LiveReading
import com.airmouse.diagnostics.live.LiveSensorCollector
import android.hardware.SensorManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiagnosticsViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = SensorScanner(app)
    private val collector = LiveSensorCollector(app.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager)

    private val _report = MutableStateFlow<CapabilityReport?>(null)
    val report: StateFlow<CapabilityReport?> = _report

    val gyro: Flow<LiveReading?> = liveOrNull(android.hardware.Sensor.TYPE_GYROSCOPE)
    val accel: Flow<LiveReading?> = liveOrNull(android.hardware.Sensor.TYPE_ACCELEROMETER)

    init {
        viewModelScope.launch { _report.value = scanner.scan() }
    }

    /** Null when the sensor is missing, so the UI can show a clear fallback message. */
    private fun liveOrNull(type: Int): Flow<LiveReading?> =
        flow {
            collector.readings(type).catch { emit(null) }.collect { emit(it) }
        }
}
