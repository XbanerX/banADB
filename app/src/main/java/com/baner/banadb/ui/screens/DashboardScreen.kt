package com.baner.banadb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun DashboardScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val data by viewModel.dashboardData.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var clipboardInput by remember { mutableStateOf("") }
    var showRebootConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshDashboard()
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "Device Dashboard",
                connectionState = connectionState,
                onDisconnect = {
                    viewModel.disconnect()
                    onBack()
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BananaGold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BananaGold)
                    }
                }
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Battery Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = BananaGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Battery Diagnostics", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BananaGold, strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Level circle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(BananaGold.copy(alpha = 0.15f))
                                .border(2.dp, BananaGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (data.batteryLevel >= 0) "${data.batteryLevel}%" else "--",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = BananaGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(data.batteryStatus, fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        // Battery Specs
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetricRow("Temp", if (data.batteryTemp > 0) "${data.batteryTemp} °C" else "N/A")
                            MetricRow("Voltage", if (data.batteryVoltage > 0) "${data.batteryVoltage} mV" else "N/A")
                            MetricRow("Health", data.batteryHealth)
                        }
                    }
                }
            }

            // Hardware & OS Specs Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Device Specifications", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailItem("Model", data.model)
                    DetailItem("Manufacturer", data.manufacturer.ifEmpty { "Android" })
                    DetailItem("Android Version", "Android ${data.androidVersion} (API ${data.sdkVersion})")
                    DetailItem("Resolution", "${data.screenWidth} x ${data.screenHeight} px")
                    DetailItem("Uptime", data.uptime.ifEmpty { "Active" })
                    DetailItem("Connected Host", data.ipAddress)
                }
            }

            // Remote Clipboard Sync Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = NeonEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remote Clipboard", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clipboardInput,
                        onValueChange = { clipboardInput = it },
                        placeholder = { Text("Paste or type text to sync...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = SurfaceCardHover
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.syncClipboardFromRemote { text ->
                                    clipboardInput = text
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonEmerald)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Remote", color = NeonEmerald, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (clipboardInput.isNotEmpty()) {
                                    viewModel.syncClipboardToRemote(clipboardInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Remote", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick System Actions & Reboot
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = StatusError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Power & Reboot Controls", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRebootConfirm = "" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Reboot", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRebootConfirm = "recovery" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Recovery", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRebootConfirm = "bootloader" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Bootloader", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Reboot confirmation dialog
    showRebootConfirm?.let { mode ->
        val modeTitle = if (mode.isEmpty()) "Reboot Device" else "Reboot to $mode"
        AlertDialog(
            onDismissRequest = { showRebootConfirm = null },
            title = { Text(modeTitle) },
            text = { Text("Are you sure you want to reboot the remote device? The ADB connection will be closed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rebootDevice(mode)
                        showRebootConfirm = null
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Reboot Now", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp))
        Text(
            value,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
