package com.baner.banadb.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.data.model.DiscoveryDevice
import com.baner.banadb.ui.components.PinInputBoxes
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun PairingDialog(
    viewModel: AdbViewModel,
    onDismiss: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pair (Android 11+), 1: Direct Connect, 2: Discovered

    // Pairing fields
    var ipInput by remember { mutableStateOf("192.168.") }
    var pairingPortInput by remember { mutableStateOf("") }
    var connectionPortInput by remember { mutableStateOf("") }
    var pairingCodeInput by remember { mutableStateOf("") }

    // Direct connect fields
    var directIpInput by remember { mutableStateOf("192.168.") }
    var directPortInput by remember { mutableStateOf("5555") }

    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    // Dismiss dialog automatically when connected
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, BananaGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BananaGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = BananaGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Connect to Device", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Wireless Debugging / ADB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceCard,
                    contentColor = BananaGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BananaGold
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pair (Android 11+)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Direct Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Nearby (${discoveredDevices.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error / Progress Banner
                when (val state = connectionState) {
                    is ConnectionState.Pairing -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusPairing.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = StatusPairing, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(state.message, color = StatusPairing, fontSize = 13.sp)
                            }
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberCyan.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberCyan, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(state.message, color = CyberCyan, fontSize = 13.sp)
                            }
                        }
                    }
                    is ConnectionState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusError.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(state.message, color = StatusError, fontSize = 13.sp)
                            }
                        }
                    }
                    else -> {}
                }

                // Tab Content
                when (selectedTab) {
                    0 -> { // Android 11+ Pairing
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceCard,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Pairing Instructions:", color = BananaGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("1. On target phone: Developer Options -> Wireless debugging -> ON", color = TextSecondary, fontSize = 12.sp)
                                        Text("2. Tap 'Pair device with pairing code'", color = TextSecondary, fontSize = 12.sp)
                                        Text("3. Enter IP, Pairing Port and 6-digit Code below", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = ipInput,
                                    onValueChange = { ipInput = it },
                                    label = { Text("Target IP Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BananaGold,
                                        unfocusedBorderColor = SurfaceCardHover,
                                        focusedLabelColor = BananaGold,
                                        unfocusedLabelColor = TextMuted
                                    )
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pairingPortInput,
                                        onValueChange = { pairingPortInput = it },
                                        label = { Text("Pairing Port") },
                                        placeholder = { Text("e.g. 37821") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BananaGold,
                                            unfocusedBorderColor = SurfaceCardHover,
                                            focusedLabelColor = BananaGold
                                        )
                                    )
                                    OutlinedTextField(
                                        value = connectionPortInput,
                                        onValueChange = { connectionPortInput = it },
                                        label = { Text("Main Port (opt)") },
                                        placeholder = { Text("e.g. 41235") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BananaGold,
                                            unfocusedBorderColor = SurfaceCardHover,
                                            focusedLabelColor = BananaGold
                                        )
                                    )
                                }
                            }

                            item {
                                Text("6-Digit Pairing Code", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = pairingCodeInput,
                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pairingCodeInput = it },
                                    label = { Text("Code") },
                                    placeholder = { Text("123456") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BananaGold,
                                        unfocusedBorderColor = SurfaceCardHover,
                                        focusedLabelColor = BananaGold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val pPort = pairingPortInput.toIntOrNull() ?: 0
                                val cPort = connectionPortInput.toIntOrNull() ?: pPort
                                if (ipInput.isNotBlank() && pPort > 0 && pairingCodeInput.length == 6) {
                                    viewModel.doPairing(ipInput.trim(), pPort, pairingCodeInput.trim(), cPort) {}
                                }
                            },
                            enabled = connectionState !is ConnectionState.Pairing && connectionState !is ConnectionState.Connecting,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                        ) {
                            Text("Pair and Connect", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    1 -> { // Direct Connect
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceCard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Direct ADB Connection:", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("For devices already paired, or devices listening on port 5555 (set via 'adb tcpip 5555' or rooted devices).", color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            OutlinedTextField(
                                value = directIpInput,
                                onValueChange = { directIpInput = it },
                                label = { Text("Device IP Address") },
                                placeholder = { Text("192.168.1.50") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = SurfaceCardHover,
                                    focusedLabelColor = CyberCyan
                                )
                            )

                            OutlinedTextField(
                                value = directPortInput,
                                onValueChange = { directPortInput = it },
                                label = { Text("ADB Port") },
                                placeholder = { Text("5555") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = SurfaceCardHover,
                                    focusedLabelColor = CyberCyan
                                )
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    val port = directPortInput.toIntOrNull() ?: 5555
                                    if (directIpInput.isNotBlank()) {
                                        viewModel.doConnect(directIpInput.trim(), port)
                                    }
                                },
                                enabled = connectionState !is ConnectionState.Connecting,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Text("Connect Directly", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    2 -> { // Discovered Devices via mDNS
                        if (discoveredDevices.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = BananaGold, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Scanning local Wi-Fi for ADB services...", color = TextSecondary, fontSize = 13.sp)
                                    Text("(Make sure Wireless Debugging is active)", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(discoveredDevices) { dev ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (dev.isPaired) {
                                                directIpInput = dev.ip
                                                directPortInput = dev.port.toString()
                                                selectedTab = 1
                                            } else {
                                                ipInput = dev.ip
                                                pairingPortInput = dev.port.toString()
                                                selectedTab = 0
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(if (dev.isPaired) StatusConnected.copy(alpha = 0.2f) else BananaGold.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (dev.isPaired) Icons.Default.CheckCircle else Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = if (dev.isPaired) StatusConnected else BananaGold,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(dev.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text("${dev.ip}:${dev.port}", color = TextSecondary, fontSize = 12.sp)
                                                    Text(
                                                        if (dev.isPaired) "Ready to connect" else "Pairing service detected",
                                                        color = if (dev.isPaired) StatusConnected else StatusPairing,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
