package com.airmouse.diagnostics.live

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Axis-labelled live reading from one sensor. */
data class LiveReading(
    val sensorName: String,
    val values: FloatArray,
    val timestampNs: Long,
    val accuracy: Int
)

/** Emits live sensor readings as a cold Flow; unregisters the listener when collection stops. */
class LiveSensorCollector(private val sensorManager: SensorManager) {

    fun readings(type: Int, samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME): Flow<LiveReading> =
        callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(LiveReading(event.sensor.name, event.values.copyOf(), event.timestamp, accuracy(event.accuracy)))
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
            }
            val registered = sensorManager.getDefaultSensor(type)?.let {
                sensorManager.registerListener(listener, it, samplingPeriodUs)
            } ?: false
            if (!registered) close(java.util.NoSuchElementException("No sensor of type $type"))
            awaitClose { sensorManager.unregisterListener(listener) }
        }

    private fun accuracy(value: Int) = when (value) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 3
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 2
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 1
        else -> 0
    }
}
