package com.baner.banadb.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.data.model.DiscoveryDevice
import com.baner.banadb.ui.components.PinInputBoxes
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingDialog(
    viewModel: AdbViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Android 11+, 1: Android 10, 2: Local (Host)

    // Android 11+ auto-discovered device pairing state
    var selectedPairingDevice by remember { mutableStateOf<DiscoveryDevice?>(null) }
    var autoPairingCode by remember { mutableStateOf("") }

    // Android 11+ manual entry fields
    var showManualPairingEntry by remember { mutableStateOf(false) }
    var ipInput by remember { mutableStateOf("192.168.") }
    var pairingPortInput by remember { mutableStateOf("") }
    var connectionPortInput by remember { mutableStateOf("") }
    var pairingCodeInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Android 10 direct connect fields
    var showManualAdb10Entry by remember { mutableStateOf(false) }
    var directIpInput by remember { mutableStateOf("192.168.") }
    var directPortInput by remember { mutableStateOf("5555") }

    // Localhost fields
    val isLocalPairingRunning by viewModel.localPairingState.isRunning.collectAsState()
    val discoveredPairingPort by viewModel.localPairingState.discoveredPairingPort.collectAsState()
    val discoveredConnectPort by viewModel.localPairingState.discoveredConnectPort.collectAsState()
    val localStatusMsg by viewModel.localPairingState.statusMessage.collectAsState()
    val isLocalPairingInProgress by viewModel.localPairingState.isPairingInProgress.collectAsState()
    val localPairingError by viewModel.localPairingState.lastError.collectAsState()
    val isLocalScanning by viewModel.isLocalScanning.collectAsState()

    var localPairingPortInput by remember { mutableStateOf("") }
    var localPairingCodeInput by remember { mutableStateOf("") }
    var localConnectionPortInput by remember { mutableStateOf("") }
    var localDirectPortInput by remember { mutableStateOf("5555") }
    var selectedLocalIp by remember { mutableStateOf(viewModel.getPreferredLocalIp()) }
    var showManualLocalPairing by remember { mutableStateOf(false) }

    LaunchedEffect(discoveredPairingPort) {
        if (discoveredPairingPort != null && localPairingPortInput.isBlank()) {
            localPairingPortInput = discoveredPairingPort.toString()
        }
    }

    LaunchedEffect(discoveredConnectPort) {
        if (discoveredConnectPort != null) {
            if (localConnectionPortInput.isBlank()) {
                localConnectionPortInput = discoveredConnectPort.toString()
            }
            if (localDirectPortInput == "5555") {
                localDirectPortInput = discoveredConnectPort.toString()
            }
        }
    }

    LaunchedEffect(discoveredDevices) {
        val localIps = viewModel.getLocalIpv4Addresses() + "127.0.0.1"
        val localService = discoveredDevices.firstOrNull { !it.isPairing && (localIps.contains(it.ip) || it.ip.startsWith("127.")) }
        if (localService != null && localDirectPortInput == "5555") {
            localDirectPortInput = localService.port.toString()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    // Pairing devices list
    val pairingDevices = discoveredDevices.filter { it.isPairing }

    // Auto-fill connection port when connect service is discovered for target IP
    LaunchedEffect(discoveredDevices, ipInput) {
        if (connectionPortInput.isBlank() && ipInput.isNotBlank()) {
            val connectDev = discoveredDevices.find { it.ip == ipInput.trim() && it.isPaired }
            if (connectDev != null) {
                connectionPortInput = connectDev.port.toString()
            }
        }
    }

    // Dismiss dialog automatically when connected
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CyberCyan.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .padding(horizontal = 16.dp)
                .imePadding()
                .padding(bottom = 8.dp)
        ) {
            // Header Title Matching Screenshots 3, 4, 5
            Text(
                text = "Find & Connect Device",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Tabs Row: [ Android 11+ ]  [ Android 10 ]  [ Local (Host) ]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberCyan,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = SurfaceCardHover) }
            ) {
                val tabs = listOf("Android 11+", "Android 10", "Local (Host)")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (selectedTab == index) CyberCyan else TextMuted
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (selectedTab) {
                0 -> { // Android 11+ (Matching Screenshot 3 & 8d1a6c8e)
                    val scrollState = rememberScrollState()
                    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

                    LaunchedEffect(imeBottom, selectedPairingDevice) {
                        if (selectedPairingDevice != null) {
                            delay(180)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pairingDevices.isEmpty()) {
                            // Searching Status Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = CyberCyan,
                                        modifier = Modifier.size(44.dp),
                                        strokeWidth = 3.dp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Searching for Android 11+ Wireless Debugging devices...",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "On target phone: Settings -> Developer Options -> Turn ON Wireless Debugging -> Tap 'Pair device with pairing code'.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            // Section Header Matching Screenshot: "Discovered Pairing Devices (6-Digit Code):"
                            Text(
                                text = "Discovered Pairing Devices (6-Digit Code):",
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )

                            pairingDevices.forEach { dev ->
                                val isSelected = selectedPairingDevice?.ip == dev.ip && selectedPairingDevice?.port == dev.port
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) CyberCyan else CyberCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            selectedPairingDevice = dev
                                            autoPairingCode = ""
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhoneAndroid,
                                                contentDescription = null,
                                                tint = CyberCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = "${dev.ip}:${dev.port}",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🔑 ", fontSize = 11.sp)
                                                    Text(
                                                        "Tap to enter 6-Digit Pairing Code",
                                                        color = CyberCyan,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "(${dev.name.ifEmpty { "adb-device" }})",
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                selectedPairingDevice = dev
                                                autoPairingCode = ""
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan,
                                                contentColor = BackgroundDark
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                "Pair Code",
                                                color = BackgroundDark,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            // Card 2: Active Pairing Code Card (Matching Screenshot 8d1a6c8e)
                            selectedPairingDevice?.let { targetDev ->
                                val activeDev = pairingDevices.firstOrNull { it.ip == targetDev.ip } ?: targetDev
                                key(activeDev.ip, activeDev.port) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.5.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.VpnKey,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Pairing with ${activeDev.ip}:${activeDev.port}",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }

                                            Text(
                                                text = "Enter 6-Digit Wi-Fi Pairing Code:",
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )

                                            // Auto-focuses first box and automatically opens soft keyboard
                                            PinInputBoxes(
                                                pinLength = 6,
                                                onPinChange = { code ->
                                                    autoPairingCode = code
                                                },
                                                onPinComplete = { code ->
                                                    autoPairingCode = code
                                                    val connectDev = discoveredDevices.firstOrNull { it.ip == activeDev.ip && !it.isPairing }
                                                    val cPort = connectDev?.port ?: 5555
                                                    viewModel.pairAndConnect(activeDev.ip, activeDev.port, code, cPort)
                                                }
                                            )

                                            val isPairing = connectionState is ConnectionState.Pairing
                                            val isReady = autoPairingCode.length == 6 && !isPairing

                                            Button(
                                                onClick = {
                                                    if (autoPairingCode.length == 6) {
                                                        val connectDev = discoveredDevices.firstOrNull { it.ip == activeDev.ip && !it.isPairing }
                                                        val cPort = connectDev?.port ?: 5555
                                                        viewModel.pairAndConnect(activeDev.ip, activeDev.port, autoPairingCode, cPort)
                                                    }
                                                },
                                                enabled = isReady,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyberCyan,
                                                    contentColor = BackgroundDark,
                                                    disabledContainerColor = SurfaceCardHover,
                                                    disabledContentColor = TextMuted
                                                )
                                            ) {
                                                if (isPairing) {
                                                    CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Pairing...", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text(
                                                        "Pair & Connect",
                                                        color = if (isReady) BackgroundDark else TextMuted,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                when (val state = connectionState) {
                                                    is ConnectionState.Pairing -> {
                                                        Text(state.message, color = CyberCyan, fontSize = 12.sp)
                                                    }
                                                    is ConnectionState.Error -> {
                                                        Text(state.message, color = StatusError, fontSize = 12.sp)
                                                    }
                                                    else -> {
                                                        Text("Ready", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Manual IP & Port Entry Toggle Link
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualPairingEntry = !showManualPairingEntry }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (showManualPairingEntry) "Hide Manual Entry" else "Manual IP & Port Entry",
                                    color = BananaGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showManualPairingEntry) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = BananaGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Animated Manual Entry Card
                        AnimatedVisibility(
                            visible = showManualPairingEntry,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BananaGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Manual Pairing Setup", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    OutlinedTextField(
                                        value = ipInput,
                                        onValueChange = { ipInput = it; validationError = null },
                                        label = { Text("Target Phone IP") },
                                        placeholder = { Text("192.168.1.100") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BananaGold,
                                            focusedLabelColor = BananaGold
                                        )
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = pairingPortInput,
                                            onValueChange = { pairingPortInput = it; validationError = null },
                                            label = { Text("Pairing Port") },
                                            placeholder = { Text("38491") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BananaGold,
                                                focusedLabelColor = BananaGold
                                            )
                                        )

                                        OutlinedTextField(
                                            value = connectionPortInput,
                                            onValueChange = { connectionPortInput = it },
                                            label = { Text("Connect Port") },
                                            placeholder = { Text("Auto / 38091") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                focusedLabelColor = CyberCyan
                                            )
                                        )
                                    }

                                    Column {
                                        Text("6-Digit Pairing Code", color = TextSecondary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        PinInputBoxes(pinLength = 6) { code ->
                                            pairingCodeInput = code
                                        }
                                    }

                                    validationError?.let { err ->
                                        Text(err, color = StatusError, fontSize = 12.sp)
                                    }

                                    val isPairing = connectionState is ConnectionState.Pairing
                                    Button(
                                        onClick = {
                                            val ip = ipInput.trim()
                                            val pPort = pairingPortInput.trim().toIntOrNull()
                                            val cPort = connectionPortInput.trim().toIntOrNull() ?: 5555
                                            if (ip.isBlank() || !ip.contains(".")) {
                                                validationError = "Please enter a valid IP address"
                                                return@Button
                                            }
                                            if (pPort == null || pPort <= 0) {
                                                validationError = "Please enter a valid Pairing Port"
                                                return@Button
                                            }
                                            if (pairingCodeInput.length != 6) {
                                                validationError = "Please enter 6-digit Pairing Code"
                                                return@Button
                                            }
                                            viewModel.pairAndConnect(ip, pPort, pairingCodeInput, cPort)
                                        },
                                        enabled = !isPairing,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                                    ) {
                                        if (isPairing) {
                                            CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pairing...", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = BackgroundDark)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pair & Connect", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Android 10 (Matching Screenshot 4)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Discovered Android 10 Devices (Port 5555):",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )

                        // Discovered / Connected devices on Port 5555
                        val adb10Devices = discoveredDevices.filter { it.isPaired || it.port == 5555 }
                        if (adb10Devices.isNotEmpty()) {
                            adb10Devices.forEach { dev ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BananaGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Usb,
                                                contentDescription = null,
                                                tint = CyberCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = "${dev.ip}:${dev.port}",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text("Direct ADB Connect", color = CyberCyan, fontSize = 12.sp, maxLines = 1)
                                                Text(
                                                    "(${dev.name.ifEmpty { "adb-device" }})",
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.connect(dev.ip, dev.port)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                "Connect",
                                                color = BackgroundDark,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.DevicesOther, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Scanning for Port 5555 Devices on LAN...", color = TextSecondary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.startDiscovery() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardHover)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = BananaGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Re-scan Network", color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Manual IP & Port Entry Link Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualAdb10Entry = !showManualAdb10Entry }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (showManualAdb10Entry) "Hide Manual Entry" else "Manual IP & Port Entry",
                                    color = BananaGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showManualAdb10Entry) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = BananaGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showManualAdb10Entry,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BananaGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Direct IP & Port Entry", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    OutlinedTextField(
                                        value = directIpInput,
                                        onValueChange = { directIpInput = it },
                                        label = { Text("Device IP Address") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BananaGold,
                                            focusedLabelColor = BananaGold
                                        )
                                    )

                                    OutlinedTextField(
                                        value = directPortInput,
                                        onValueChange = { directPortInput = it },
                                        label = { Text("Port (default 5555)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BananaGold,
                                            focusedLabelColor = BananaGold
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val ip = directIpInput.trim()
                                            val port = directPortInput.trim().toIntOrNull() ?: 5555
                                            viewModel.connect(ip, port)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Connect Device", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> { // Local (Host) - Matching User Screenshots & Automatic Notification Flow
                    val isConnected = connectionState is ConnectionState.Connected
                    val connectedDevName = if (connectionState is ConnectionState.Connected) (connectionState as ConnectionState.Connected).deviceName else ""
                    val connectedHost = if (connectionState is ConnectionState.Connected) (connectionState as ConnectionState.Connected).ip else ""
                    val connectedPort = if (connectionState is ConnectionState.Connected) (connectionState as ConnectionState.Connected).port else 0

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card 1: Local ADB Status Card (Matching Screenshot 1)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceCardHover),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isConnected) StatusConnected else TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isConnected) "Connected: $connectedDevName" else "Local ADB: Disconnected",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isConnected) "$connectedHost:$connectedPort" else "Disconnected",
                                        color = if (isConnected) CyberCyan else TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                                if (isConnected) {
                                    OutlinedButton(
                                        onClick = { viewModel.disconnect() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Disconnect", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Card 2: Direct Connect to Localhost (Matching Screenshot 1)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Usb,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Direct Connect to Localhost",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Text(
                                    text = "Developer Options -> Wireless Debugging shows:\n'IP address & Port: 192.168.x.x:PORT'. Enter that PORT below:",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                OutlinedTextField(
                                    value = localDirectPortInput,
                                    onValueChange = { localDirectPortInput = it },
                                    label = { Text("Connection Port (e.g. 38491)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        focusedLabelColor = CyberCyan
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val p = localDirectPortInput.trim().toIntOrNull() ?: 5555
                                            viewModel.connect(selectedLocalIp, p)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                    ) {
                                        Text("Connect", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.scanAndConnectLocal { detectedPort ->
                                                localDirectPortInput = detectedPort.toString()
                                            }
                                        },
                                        enabled = !isLocalScanning,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(CyberCyan.copy(alpha = 0.5f))
                                        )
                                    ) {
                                        if (isLocalScanning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = CyberCyan,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Scanning...", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Auto-Scan", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        // Section 3: Discovered Local ADB Services (Matching Screenshot 1 & 2)
                        val localServices = discoveredDevices.filter { !it.isPairing }
                        Text(
                            text = "Discovered Local ADB Services:",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )

                        if (localServices.isEmpty()) {
                            Text(
                                text = "No local services detected yet. Tap 'Auto-Scan' or turn ON Wireless Debugging.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        } else {
                            localServices.forEach { service ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PhoneAndroid,
                                                contentDescription = null,
                                                tint = CyberCyan,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${service.ip}:${service.port}",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = service.name,
                                                    color = CyberCyan,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.connect(service.ip, service.port)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                                        ) {
                                            Text("Connect", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Card 4: Pair Local Device (Automatic via Notification) (Matching Screenshot 2)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isLocalPairingRunning) CyberCyan else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Pair Local Device (Automatic via Notification)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Text(
                                    text = "1. Tap 'Start Service' below.\n" +
                                           "2. Tap 'Open Developer Options' -> 'Wireless Debugging'.\n" +
                                           "3. Tap 'Pair device with pairing code'.\n" +
                                           "4. Enter the 6-digit code in notification and hit Send!\n" +
                                           "5. When paired, tap notification or return here to connect.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                if (isLocalPairingRunning) {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(StatusConnected)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Service Active: $localStatusMsg",
                                                    color = CyberCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            if (discoveredPairingPort != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Discovered Pairing Port: $discoveredPairingPort",
                                                    color = BananaGold,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

                                if (!isLocalPairingRunning) {
                                    // Start Service Button
                                    Button(
                                        onClick = {
                                            viewModel.startLocalPairingService(context)
                                            Toast.makeText(context, "Pairing service started! Check notifications.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Local Pairing Service", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                } else {
                                    // Primary Button immediately transforms to Open Developer Options
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                try {
                                                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Could not open Settings", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Developer Options", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    // Secondary Stop Service Button
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.stopLocalPairingService(context)
                                            Toast.makeText(context, "Pairing service stopped", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(StatusError.copy(alpha = 0.5f))
                                        )
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Stop Service", color = StatusError, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }

                                // Overlay permission button for Auto-Return
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            try {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                ).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (canOverlay) StatusConnected else CyberCyan
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (canOverlay) StatusConnected.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.5f)
                                        )
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (canOverlay) Icons.Default.CheckCircle else Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (canOverlay) StatusConnected else CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (canOverlay) "Auto-Return to App Enabled" else "Allow Auto-Return to App (Overlay)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Collapsible Manual Local Pairing Section (Fallback)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualLocalPairing = !showManualLocalPairing }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (showManualLocalPairing) "Hide Manual Local Pairing" else "Manual Local Pairing (Split-Screen)",
                                    color = BananaGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showManualLocalPairing) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = BananaGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (showManualLocalPairing) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Manual Localhost Pairing", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    // IP selection
                                    Text("Target Interface:", color = TextMuted, fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val detectedIp = viewModel.getPreferredLocalIp()
                                        listOf(detectedIp, "127.0.0.1").distinct().forEach { candidateIp ->
                                            val isSelected = selectedLocalIp == candidateIp
                                            OutlinedButton(
                                                onClick = { selectedLocalIp = candidateIp },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
                                                    contentColor = if (isSelected) CyberCyan else TextSecondary
                                                ),
                                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CyberCyan else SurfaceCardHover)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (candidateIp == "127.0.0.1") "127.0.0.1" else "Wi-Fi: $candidateIp",
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Inputs: Pairing Port & Pairing Code
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = localPairingPortInput,
                                            onValueChange = { localPairingPortInput = it },
                                            label = { Text("Pairing Port", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. 38491", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                focusedLabelColor = CyberCyan
                                            )
                                        )

                                        OutlinedTextField(
                                            value = localPairingCodeInput,
                                            onValueChange = { localPairingCodeInput = it },
                                            label = { Text("Pairing Code", fontSize = 11.sp) },
                                            placeholder = { Text("6 digits", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                focusedLabelColor = CyberCyan
                                            )
                                        )
                                    }

                                    // Input: Connection Port
                                    OutlinedTextField(
                                        value = localConnectionPortInput,
                                        onValueChange = { localConnectionPortInput = it },
                                        label = { Text("Connection Port (from main Wireless Debugging screen)") },
                                        placeholder = { Text("e.g. 41235") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            focusedLabelColor = CyberCyan
                                        )
                                    )

                                    if (localPairingError != null) {
                                        Text(
                                            text = "Error: $localPairingError",
                                            color = StatusError,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val pPort = localPairingPortInput.trim().toIntOrNull()
                                            val cPort = localConnectionPortInput.trim().toIntOrNull() ?: 5555
                                            val code = localPairingCodeInput.trim()

                                            if (pPort == null || code.length < 6) {
                                                Toast.makeText(context, "Enter a valid pairing port and 6-digit code", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.pairLocalDevice(
                                                    ip = selectedLocalIp,
                                                    pairingPort = pPort,
                                                    pairingCode = code,
                                                    connectionPort = cPort
                                                ) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "Paired and connected to local device!", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isLocalPairingInProgress,
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                                    ) {
                                        if (isLocalPairingInProgress) {
                                            CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pairing with Local Device...", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Pair & Connect Local Device", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
