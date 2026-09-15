package com.airmouse.diagnostics.diagnostics

enum class CheckStatus { PASS, WARN, FAIL, UNKNOWN }

data class Check(
    val key: String,
    val label: String,
    val status: CheckStatus,
    val detail: String
)

/** Result of a device-capability scan. Pure data — no Android objects — so it is unit-testable. */
data class CapabilityReport(
    val checks: List<Check>,
    val sensorReports: Map<String, SensorReport>,
    val hidSupport: Boolean,
    val hidUnavailableReason: String?
)
