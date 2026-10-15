package com.baner.banadb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baner.banadb.ui.screens.*
import com.baner.banadb.ui.theme.BackgroundDark
import com.baner.banadb.ui.theme.BanaAdbTheme
import com.baner.banadb.ui.viewmodel.AdbViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BanaAdbTheme {
                val viewModel: AdbViewModel = viewModel()
                var currentRoute by remember { mutableStateOf("home") }
                var showPairingDialog by remember { mutableStateOf(false) }

                BackHandler(enabled = currentRoute != "home") {
                    currentRoute = "home"
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    when (currentRoute) {
                        "home" -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenPairing = { showPairingDialog = true },
                                onNavigateToSection = { route -> currentRoute = route }
                            )
                        }
                        "control" -> {
                            RemoteControlScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                        "terminal" -> {
                            TerminalScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                        "apps" -> {
                            AppManagerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                        "files" -> {
                            FileManagerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                        "processes" -> {
                            ProcessManagerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                        "scripts" -> {
                            ScriptRunnerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" },
                                onNavigateToTerminal = { currentRoute = "terminal" }
                            )
                        }
                        "mirror" -> {
                            ScreenMirrorScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = "home" }
                            )
                        }
                    }

                    if (showPairingDialog) {
                        PairingDialog(
                            viewModel = viewModel,
                            onDismiss = { showPairingDialog = false }
                        )
                    }
                }
            }
        }
    }
}
