package com.baner.banadb.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.data.model.SavedDevice
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun HomeScreen(
    viewModel: AdbViewModel,
    onOpenPairing: () -> Unit,
    onNavigateToSection: (String) -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "BanaADB",
                connectionState = connectionState,
                onDisconnect = { viewModel.disconnect() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenPairing,
                containerColor = BananaGold,
                contentColor = BackgroundDark,
                icon = { Icon(Icons.Default.Add, "Pair") },
                text = { Text("Pair New Device", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Connection Status", color = TextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                when (val state = connectionState) {
                                    is ConnectionState.Connected -> {
                                        Text(
                                            text = state.deviceName,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = BananaGold
                                        )
                                        Text(
                                            text = "${state.ip}:${state.port}",
                                            color = StatusConnected,
                                            fontSize = 13.sp
                                        )
                                    }
                                    is ConnectionState.Connecting -> {
                                        Text("Connecting...", style = MaterialTheme.typography.titleMedium, color = StatusPairing)
                                        Text(state.message, color = TextMuted, fontSize = 12.sp)
                                    }
                                    is ConnectionState.Pairing -> {
                                        Text("Pairing in progress...", style = MaterialTheme.typography.titleMedium, color = StatusPairing)
                                        Text(state.message, color = TextMuted, fontSize = 12.sp)
                                    }
                                    is ConnectionState.Error -> {
                                        Text("Connection Error", style = MaterialTheme.typography.titleMedium, color = StatusError)
                                        Text(state.message, color = TextMuted, fontSize = 12.sp)
                                    }
                                    ConnectionState.Disconnected -> {
                                        Text("Disconnected", style = MaterialTheme.typography.titleLarge, color = TextMuted)
                                        Text("Connect via Wi-Fi ADB to start controlling", color = TextSecondary, fontSize = 13.sp)
                                    }
                                }
                            }

                            if (connectionState is ConnectionState.Connected) {
                                Button(
                                    onClick = { onNavigateToSection("control") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("Control", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Tool shortcuts if connected
            if (connectionState is ConnectionState.Connected) {
                item {
                    Text("Device Utilities", style = MaterialTheme.typography.titleMedium, color = BananaGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "Terminal",
                            icon = Icons.Default.Terminal,
                            color = CyberCyan,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("terminal") }
                        )
                        QuickActionCard(
                            title = "Dashboard",
                            icon = Icons.Default.Dashboard,
                            color = NeonEmerald,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("dashboard") }
                        )
                        QuickActionCard(
                            title = "Apps",
                            icon = Icons.Default.Apps,
                            color = BananaGold,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("apps") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "Files",
                            icon = Icons.Default.Folder,
                            color = BananaGoldLight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("files") }
                        )
                        QuickActionCard(
                            title = "Processes",
                            icon = Icons.Default.Memory,
                            color = CyberCyanLight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("processes") }
                        )
                        QuickActionCard(
                            title = "Mirror",
                            icon = Icons.Default.Screenshot,
                            color = StatusConnected,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("mirror") }
                        )
                    }
                }
            }

            // Host Developer Options Button
            item {
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceCardHover))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = BananaGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Developer Options (This Phone)")
                }
            }

            // Saved Devices Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Saved Devices", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "${savedDevices.size} Saved",
                        color = BananaGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (savedDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Devices,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No saved devices yet", color = TextSecondary, fontSize = 14.sp)
                            Text("Tap '+ Pair New Device' to scan or add", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(savedDevices) { device ->
                    SavedDeviceCard(
                        device = device,
                        onConnect = { viewModel.doConnect(device.ip, device.port) },
                        onDelete = { viewModel.deleteSavedDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SavedDeviceCard(
    device: SavedDevice,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BananaGold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(device.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${device.ip}:${device.port}", color = TextSecondary, fontSize = 13.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Connect", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError.copy(alpha = 0.8f))
                }
            }
        }
    }
}
