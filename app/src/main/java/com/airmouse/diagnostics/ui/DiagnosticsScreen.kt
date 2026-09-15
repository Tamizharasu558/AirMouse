package com.airmouse.diagnostics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.diagnostics.diagnostics.CheckStatus
import com.airmouse.diagnostics.diagnostics.CapabilityReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel) {
    val report by viewModel.report.collectAsStateWithLifecycle(initialValue = null)
    val gyro by viewModel.gyro.collectAsStateWithLifecycle(initialValue = null)
    val accel by viewModel.accel.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(topBar = { TopAppBar(title = { Text("Air Mouse Diagnostics") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (report == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CapabilitySection(report!!)
                LiveSensorSection(gyro, accel)
            }
        }
    }
}

@Composable
fun CapabilitySection(report: CapabilityReport) {
    Text("Capabilities", style = MaterialTheme.typography.titleLarge)
    report.checks.forEach { check -> CheckRow(check) }
    Text(
        when {
            report.hidSupport -> "Device meets API requirements for Bluetooth HID mouse mode (Phase 2 candidate)."
            else -> report.hidUnavailableReason ?: "HID support undetermined."
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun CheckRow(check: com.airmouse.diagnostics.diagnostics.Check) {
    val color = when (check.status) {
        CheckStatus.PASS -> MaterialTheme.colorScheme.primary
        CheckStatus.WARN -> MaterialTheme.colorScheme.tertiary
        CheckStatus.FAIL -> MaterialTheme.colorScheme.error
        CheckStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (check.status) {
                CheckStatus.PASS -> "✓"
                CheckStatus.WARN -> "!"
                CheckStatus.FAIL -> "✗"
                CheckStatus.UNKNOWN -> "?"
            },
            color = color, style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(check.label, style = MaterialTheme.typography.titleSmall)
            Text(check.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LiveSensorSection(gyro: com.airmouse.diagnostics.live.LiveReading?, accel: com.airmouse.diagnostics.live.LiveReading?) {
    Text("Live readings", style = MaterialTheme.typography.titleLarge)
    if (gyro == null) Text("Gyroscope: unavailable", style = MaterialTheme.typography.bodyMedium)
    else LiveRow("Gyroscope (rad/s)", gyro)
    if (accel == null) Text("Accelerometer: unavailable", style = MaterialTheme.typography.bodyMedium)
    else LiveRow("Accelerometer (m/s²)", accel)
}

@Composable
fun LiveRow(label: String, reading: com.airmouse.diagnostics.live.LiveReading) {
    val v = reading.values
    Text("$label: x=${fmt(v.getOrNull(0))}, y=${fmt(v.getOrNull(1))}, z=${fmt(v.getOrNull(2))}",
        style = MaterialTheme.typography.bodyMedium)
}

private fun fmt(f: Float?) = f?.let { String.format("%.3f", it) } ?: "—"
