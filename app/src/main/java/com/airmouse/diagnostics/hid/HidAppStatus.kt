package com.airmouse.diagnostics.hid

/** Lifecycle of the BluetoothHidDevice app registration on this phone. */
enum class HidAppStatus {
    /** Proxy not connected / app not registered. */
    IDLE,

    /** registerApp() sent, waiting for the stack callback. */
    REGISTERING,

    /** Stack confirmed registration — input reports can be sent. */
    REGISTERED,

    /** registerApp() returned false or the stack rejected the report map. */
    REGISTRATION_FAILED
}