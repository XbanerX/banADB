package com.baner.banadb.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.baner.banadb.R
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.data.model.SavedDevice
import com.baner.banadb.data.repository.DevicePreferences
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
    val themeMode by viewModel.themeMode.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSavedDevices()
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "banADB",
                connectionState = connectionState,
                onDisconnect = { viewModel.disconnect() },
                actions = {
                    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val isDark = when (themeMode) {
                        DevicePreferences.THEME_LIGHT -> false
                        DevicePreferences.THEME_DARK -> true
                        else -> isSystemDark
                    }
                    val themeIcon = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode
                    val themeTooltip = if (isDark) stringResource(R.string.theme_dark) else stringResource(R.string.theme_light)

                    IconButton(
                        onClick = {
                            val nextMode = if (isDark) DevicePreferences.THEME_LIGHT else DevicePreferences.THEME_DARK
                            viewModel.setThemeMode(nextMode)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = themeTooltip,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.about_title),
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenPairing,
                containerColor = BananaGold,
                contentColor = BackgroundDark,
                icon = { Icon(Icons.Default.Add, stringResource(R.string.home_pair_new_device), tint = BackgroundDark) },
                text = { Text(stringResource(R.string.home_pair_new_device), color = BackgroundDark, fontWeight = FontWeight.Bold) }
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
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(end = 8.dp)
                            ) {
                                Text(stringResource(R.string.home_connection_status), color = TextSecondary, fontSize = 12.sp)
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
                                        Text(stringResource(R.string.home_status_connecting), style = MaterialTheme.typography.titleMedium, color = StatusPairing)
                                        Text(state.message, color = TextMuted, fontSize = 12.sp)
                                    }
                                    is ConnectionState.Pairing -> {
                                        Text(stringResource(R.string.home_status_pairing), style = MaterialTheme.typography.titleMedium, color = StatusPairing)
                                        Text(state.message, color = TextMuted, fontSize = 12.sp)
                                    }
                                    is ConnectionState.Error -> {
                                        Text(stringResource(R.string.home_status_error), style = MaterialTheme.typography.titleMedium, color = StatusError)
                                        Text(state.message, color = TextMuted, fontSize = 12.sp)
                                    }
                                    ConnectionState.Disconnected -> {
                                        Text(stringResource(R.string.home_status_disconnected), style = MaterialTheme.typography.titleLarge, color = TextMuted)
                                        Text(stringResource(R.string.home_disconnected_desc), color = TextSecondary, fontSize = 13.sp)
                                    }
                                }
                            }

                            if (connectionState is ConnectionState.Connected) {
                                Button(
                                    onClick = { onNavigateToSection("control") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(stringResource(R.string.home_btn_control), color = BackgroundDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Tool shortcuts if connected
            if (connectionState is ConnectionState.Connected) {
                item {
                    Text(stringResource(R.string.home_utilities), style = MaterialTheme.typography.titleMedium, color = BananaGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = stringResource(R.string.tool_terminal),
                            icon = Icons.Default.Terminal,
                            color = CyberCyan,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("terminal") }
                        )
                        QuickActionCard(
                            title = stringResource(R.string.tool_dashboard),
                            icon = Icons.Default.Dashboard,
                            color = NeonEmerald,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("dashboard") }
                        )
                        QuickActionCard(
                            title = stringResource(R.string.tool_apps),
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
                            title = stringResource(R.string.tool_files),
                            icon = Icons.Default.Folder,
                            color = BananaGoldLight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("files") }
                        )
                        QuickActionCard(
                            title = stringResource(R.string.tool_processes),
                            icon = Icons.Default.Memory,
                            color = CyberCyanLight,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSection("processes") }
                        )
                        QuickActionCard(
                            title = stringResource(R.string.tool_mirror),
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
                    Text(stringResource(R.string.home_open_dev_options))
                }
            }

            // Saved Devices Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.home_saved_devices), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = stringResource(R.string.home_saved_count, savedDevices.size),
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
                            Text(stringResource(R.string.home_no_saved_devices), color = TextSecondary, fontSize = 14.sp)
                            Text(stringResource(R.string.home_no_saved_desc), color = TextMuted, fontSize = 12.sp)
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

            // Bottom clearance for floating action button
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            viewModel = viewModel,
            onDismiss = { showAboutDialog = false }
        )
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BananaGold, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        device.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${device.ip}:${device.port}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.connect), color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = StatusError.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
