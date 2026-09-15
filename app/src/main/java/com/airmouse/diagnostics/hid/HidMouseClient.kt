package com.airmouse.diagnostics.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.airmouse.diagnostics.hid.HidAppStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor

@SuppressLint("MissingPermission")
class HidMouseClient(private val context: Context, private val adapter: BluetoothAdapter?) {

    private var proxy: BluetoothHidDevice? = null
    private var started = false
    private var lastInputReport = ByteArray(HidReportDescriptor.REPORT_SIZE)

    private val _appStatus = MutableStateFlow(HidAppStatus.IDLE)
    val appStatus: StateFlow<HidAppStatus> = _appStatus.asStateFlow()

    /** Remote host address → BluetoothProfile connection state. */
    private val _connections = MutableStateFlow<Map<String, Int>>(emptyMap())
    val connections: StateFlow<Map<String, Int>> = _connections.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** True when SecurityException was caught - the BLUETOOTH_CONNECT runtime grant is missing. */
    private val _securityFailure = MutableStateFlow(false)
    val securityFailure: StateFlow<Boolean> = _securityFailure.asStateFlow()

    private fun onSecurityFailure(e: SecurityException): Boolean {
        _securityFailure.value = true
        _lastError.value = "Missing BLUETOOTH_CONNECT runtime permission: ${e.message}"
        return false
    }

    private val callbackExecutor: Executor get() = context.mainExecutor

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, p: BluetoothProfile) {
            proxy = p as BluetoothHidDevice
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            proxy = null
            _appStatus.value = HidAppStatus.IDLE
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _appStatus.value = if (registered) HidAppStatus.REGISTERED else HidAppStatus.IDLE
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            _connections.update { m ->
                val next = m.toMutableMap()
                if (state == BluetoothProfile.STATE_DISCONNECTED) next.remove(device.address)
                else next[device.address] = state
                next
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                proxy?.replyReport(device, BluetoothHidDevice.REPORT_TYPE_INPUT, id, lastInputReport)
            } else {
                proxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray?) {
            proxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            _connections.update { it - device.address }
        }
    }

    fun start() {
        if (started || adapter == null) return
        started = true
        try {
            adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        } catch (e: SecurityException) {
            onSecurityFailure(e)
        }
    }

    fun shutdown() {
        if (!started) return
        started = false
        runCatching { proxy?.unregisterApp() }
        proxy?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) } }
        proxy = null
        _appStatus.value = HidAppStatus.IDLE
        _connections.value = emptyMap()
    }

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Air Mouse",
            "Smartphone-based wireless air mouse",
            "airmouse",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            HidReportDescriptor.MOUSE
        )
        _appStatus.value = HidAppStatus.REGISTERING
        val ok = try {
            proxy?.registerApp(sdp, null, null, callbackExecutor, callback) ?: false
        } catch (e: SecurityException) {
            onSecurityFailure(e)
            false
        }
        if (!ok && _securityFailure.value != true) {
            _appStatus.value = HidAppStatus.REGISTRATION_FAILED
            _lastError.value = "registerApp() returned false"
        }
    }

    fun bondedDevices(): List<BluetoothDevice> = try {
        adapter?.bondedDevices
            ?.sortedBy { runCatching { it.name }.getOrNull() ?: "" }
            ?: emptyList()
    } catch (e: SecurityException) {
        onSecurityFailure(e)
        emptyList()
    }

    fun connect(device: BluetoothDevice) {
        try {
            val p = proxy ?: run { _lastError.value = "HID proxy not ready yet"; return }
            if (!p.connect(device)) _lastError.value = "connect() failed — is the device bonded?"
        } catch (e: SecurityException) {
            onSecurityFailure(e)
        }
    }

    fun disconnect(device: BluetoothDevice) {
        try {
            proxy?.disconnect(device)
        } catch (e: SecurityException) {
            onSecurityFailure(e)
        } catch (e: RuntimeException) {
            _lastError.value = "disconnect error: ${e.message}"
        }
    }

    /** Sends a 4-byte input report (buttons, x, y, wheel). True when accepted by the stack. */
    fun sendInput(report: ByteArray): Boolean {
        if (_appStatus.value != HidAppStatus.REGISTERED) {
            _lastError.value = "HID app not registered"
            return false
        }
        val address = _connections.value.filterValues { it == BluetoothProfile.STATE_CONNECTED }
            .keys.firstOrNull()
            ?: run { _lastError.value = "No connected host"; return false }
        val device = try {
            adapter?.getRemoteDevice(address)
        } catch (e: SecurityException) {
            return onSecurityFailure(e)
        } catch (e: IllegalArgumentException) {
            _lastError.value = "Bad device address: $address"
            return false
        } ?: return false
        return try {
            val ok = proxy?.sendReport(device, 0, report) ?: false
            if (ok) lastInputReport = report.copyOf()
            else _lastError.value = "sendReport() rejected by stack"
            ok
        } catch (e: Exception) {
            _lastError.value = "sendReport error: ${e.message}"
            false
        }
    }
}