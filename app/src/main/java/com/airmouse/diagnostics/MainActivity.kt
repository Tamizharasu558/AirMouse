package com.airmouse.diagnostics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airmouse.diagnostics.ui.BluetoothPermissionGate
import com.airmouse.diagnostics.ui.DiagnosticsScreen
import com.airmouse.diagnostics.ui.DiagnosticsViewModel
import com.airmouse.diagnostics.ui.MouseScreen
import com.airmouse.diagnostics.ui.MouseViewModel

class MainActivity : ComponentActivity() {
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private val mouseViewModel: MouseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val tab = rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab.intValue == 0,
                                onClick = { tab.intValue = 0 },
                                icon = { Text("📊") },
                                label = { Text("Diagnostics") }
                            )
                            NavigationBarItem(
                                selected = tab.intValue == 1,
                                onClick = { tab.intValue = 1 },
                                icon = { Text("🖱") },
                                label = { Text("Mouse") }
                            )
                        }
                    }
                ) { padding ->
                    Surface(Modifier.fillMaxSize().padding(padding)) {
                        when (tab.intValue) {
                            0 -> DiagnosticsScreen(diagnosticsViewModel)
                            else -> BluetoothPermissionGate { MouseScreen(mouseViewModel) }
                        }
                    }
                }
            }
        }
    }
}
