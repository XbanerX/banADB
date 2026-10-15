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
import com.baner.banadb.data.model.ConnectionState
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
                val connectionState by viewModel.connectionState.collectAsState()

                // Auto-transition to Remote Control screen when connected
                LaunchedEffect(connectionState) {
                    if (connectionState is ConnectionState.Connected && currentRoute == "home") {
                        currentRoute = "control"
                    } else if (connectionState !is ConnectionState.Connected && currentRoute != "home") {
                        currentRoute = "home"
                    }
                }

                val parentRoute = if (connectionState is ConnectionState.Connected) "control" else "home"

                BackHandler(enabled = currentRoute != "home") {
                    if (currentRoute == "control") {
                        currentRoute = "home"
                    } else {
                        currentRoute = parentRoute
                    }
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
                                onBack = { currentRoute = "home" },
                                onNavigateToSection = { route -> currentRoute = route }
                            )
                        }
                        "terminal" -> {
                            TerminalScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
                            )
                        }
                        "apps" -> {
                            AppManagerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
                            )
                        }
                        "files" -> {
                            FileManagerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
                            )
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
                            )
                        }
                        "processes" -> {
                            ProcessManagerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
                            )
                        }
                        "scripts" -> {
                            ScriptRunnerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute },
                                onNavigateToTerminal = { currentRoute = "terminal" }
                            )
                        }
                        "mirror" -> {
                            ScreenMirrorScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
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
