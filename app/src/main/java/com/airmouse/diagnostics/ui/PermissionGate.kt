package com.airmouse.diagnostics.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Runtime gate for android.permission.BLUETOOTH_CONNECT (Android 12+).
 *
 * The manifest alone is NOT enough: Bluetooth APIs like BluetoothAdapter.bondedDevices
 * and BluetoothHidDevice.registerApp throw SecurityException when the runtime grant is
 * missing - which crashed the app on every mouse interaction before this gate existed.
 */
@Composable
fun BluetoothPermissionGate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val needed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var granted by remember {
        mutableStateOf(
            !needed || ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    if (granted) {
        content()
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Bluetooth permission needed\n\n" +
                        "The app needs the \"Nearby devices\" (BLUETOOTH_CONNECT) runtime " +
                        "permission to use Bluetooth HID. Without it every Bluetooth call " +
                        "throws SecurityException and the app exits.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Button(onClick = { launcher.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                Text("Grant permission")
            }
        }
    }
}
