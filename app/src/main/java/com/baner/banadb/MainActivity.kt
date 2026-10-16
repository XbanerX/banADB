package com.baner.banadb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.ui.screens.*
import com.baner.banadb.ui.theme.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.baner.banadb.ui.viewmodel.AdbViewModel

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        val manager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.cancel(com.baner.banadb.service.LocalPairingReceiver.NOTIFICATION_ID_CONNECTED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BanaAdbTheme {
                val viewModel: AdbViewModel = viewModel()
                var showSplash by remember { mutableStateOf(true) }
                var currentRoute by remember { mutableStateOf("home") }
                var showPairingDialog by remember { mutableStateOf(false) }
                var showExitAppDialog by remember { mutableStateOf(false) }
                val connectionState by viewModel.connectionState.collectAsState()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Result handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else {
                    // Auto-transition to Remote Control screen when connected
                    LaunchedEffect(connectionState) {
                        if (connectionState is ConnectionState.Connected && currentRoute == "home") {
                            currentRoute = "control"
                        } else if (connectionState !is ConnectionState.Connected && currentRoute != "home") {
                            currentRoute = "home"
                        }
                    }

                    val parentRoute = if (connectionState is ConnectionState.Connected) "control" else "home"

                    // Exit App confirmation on Home screen
                    BackHandler(enabled = currentRoute == "home") {
                        showExitAppDialog = true
                    }

                    // Sub-screen navigation back to parentRoute (RemoteControl handles its own back)
                    BackHandler(enabled = currentRoute != "home" && currentRoute != "control") {
                        currentRoute = parentRoute
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
                        "gallery" -> {
                            GalleryScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = parentRoute }
                            )
                        }
                        "notifications" -> {
                            NotificationMirrorScreen(
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

                    if (showExitAppDialog) {
                        val activity = LocalContext.current as? ComponentActivity
                        AlertDialog(
                            onDismissRequest = { showExitAppDialog = false },
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = BananaGold, modifier = Modifier.size(32.dp)) },
                            title = { Text("Exit banADB?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                            text = { Text("Are you sure you want to close banADB?", color = TextSecondary, fontSize = 14.sp) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showExitAppDialog = false
                                        activity?.finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitAppDialog = false }) {
                                    Text("Cancel", color = TextSecondary)
                                }
                            },
                            containerColor = SurfaceDark,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }
    }
}
}
