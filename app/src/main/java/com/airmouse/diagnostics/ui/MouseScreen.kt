package com.airmouse.diagnostics.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.diagnostics.hid.MouseReport

private const val STATE_CONNECTED = 2

@Composable
fun MouseScreen(viewModel: MouseViewModel) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val clickLog by viewModel.clickLog.collectAsStateWithLifecycle()
    val airMode by viewModel.airMode.collectAsStateWithLifecycle()
    val calProgress by viewModel.calProgress.collectAsStateWithLifecycle()
    val sensitivity by viewModel.sensitivity.collectAsStateWithLifecycle()
    val connectedLabel by viewModel.connectedLabel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Air Mouse", style = MaterialTheme.typography.headlineSmall)
        Text("HID app: ${status.name}", fontSize = 14.sp)
        connectedLabel?.let { Text("Connected host: $it", fontSize = 14.sp) }
        lastError?.let { err ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(err, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = viewModel::clearError) { Text("OK") }
                }
            }
        }

        // Touchpad: drag to move cursor; click buttons.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.clearTouchAccumulator() },
                            onDragEnd = { viewModel.clearTouchAccumulator() },
                            onDragCancel = { viewModel.clearTouchAccumulator() },
                            onDrag = { change, amount ->
                                change.consume()
                                viewModel.onTouchDelta(amount.x, amount.y)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) { Text("Drag to move cursor", fontSize = 13.sp) }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.sendPress(MouseReport.BUTTON_LEFT) }, modifier = Modifier.fillMaxWidth()) { Text("L Press") }
                Button(onClick = { viewModel.sendRelease() }, modifier = Modifier.fillMaxWidth()) { Text("L Release") }
                OutlinedButton(onClick = { viewModel.sendPress(MouseReport.BUTTON_RIGHT) }, modifier = Modifier.fillMaxWidth()) { Text("R Press") }
                Button(onClick = { viewModel.sendWheel(-1) }, modifier = Modifier.fillMaxWidth()) { Text("Wheel -1") }
                Button(onClick = { viewModel.sendWheel(1) }, modifier = Modifier.fillMaxWidth()) { Text("Wheel +1") }
            }
        }

        // Air mode: calibration chip -> live gyro movement, sensitivity slider.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = airMode == AirMode.AIR,
                onClick = viewModel::startAirCalibration,
                label = { Text(if (airMode == AirMode.CALIBRATING) "Hold still..." else "Air") }
            )
            if (airMode != AirMode.OFF) {
                FilterChip(selected = false, onClick = viewModel::stopAir, label = { Text("Stop") })
            }
        }

        if (airMode == AirMode.CALIBRATING) {
            LinearProgressIndicator(progress = { calProgress }, modifier = Modifier.fillMaxWidth())
        }

        if (airMode != AirMode.OFF) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sensitivity", fontSize = 13.sp)
                Slider(
                    value = sensitivity,
                    onValueChange = viewModel::setSensitivity,
                    valueRange = 500f..3000f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }
        }

        // Sent-report log
        Text("Last reports", style = MaterialTheme.typography.titleMedium)
        clickLog.takeLast(3).forEach { entry ->
            Text("- ${entry.text} ${if (entry.sent) "[ok]" else "[x]"}", fontSize = 13.sp)
        }
        if (clickLog.isEmpty()) Text("No reports sent yet", fontSize = 13.sp)

        // Bonded devices — every row is a tappable, >=48dp target inside the scrollable column.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bonded devices", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { viewModel.refreshDeviceList() }) { Text("Refresh") }
        }
        if (devices.isEmpty()) Text("None - bond the PC in Bluetooth settings first", fontSize = 13.sp)
        devices.forEach { row ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable { viewModel.connect(row) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.name, fontSize = 15.sp)
                        Text(row.address, fontSize = 12.sp)
                    }
                    when (row.connectionState) {
                        STATE_CONNECTED -> OutlinedButton(onClick = { viewModel.disconnect(row) }) { Text("Disc") }
                        else -> Button(onClick = { viewModel.connect(row) }) { Text("Connect") }
                    }
                }
            }
        }

        // Host pairing happens in the system Bluetooth settings (the PC pairs TO this phone).
        OutlinedButton(
            onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pair new device (open Bluetooth settings)") }
    }
}
