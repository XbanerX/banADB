package com.baner.banadb.ui.screens

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
    var localPortInput by remember { mutableStateOf("5555") }

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

                2 -> { // Local (Host) (Matching Screenshot 5)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card 1: Local ADB Status Card
                        val isLocalConnected = (connectionState is ConnectionState.Connected) &&
                                (viewModel.savedDevices.value.any { it.ip == "127.0.0.1" || it.ip == "localhost" })
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = if (isLocalConnected) StatusConnected else TextMuted)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Local ADB: " + if (isLocalConnected) "Connected" else "Disconnected",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (isLocalConnected) "Local host debugging active" else "Disconnected",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Card 2: Direct Connect to Localhost Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BananaGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Usb, contentDescription = null, tint = BananaGold, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Direct Connect to Localhost", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                Text(
                                    text = "Developer Options -> Wireless Debugging shows: 'IP address & Port: 192.168.x.x:PORT'. Enter that PORT below:",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )

                                OutlinedTextField(
                                    value = localPortInput,
                                    onValueChange = { localPortInput = it },
                                    label = { Text("Connection Port (e.g. 38491)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BananaGold,
                                        focusedLabelColor = BananaGold
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val p = localPortInput.trim().toIntOrNull() ?: 5555
                                            viewModel.connect("127.0.0.1", p)
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                                    ) {
                                        Text("Connect", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            // Scan common adb ports on localhost (5555, 37000-45000)
                                            Toast.makeText(context, "Scanning local ADB services...", Toast.LENGTH_SHORT).show()
                                            viewModel.startDiscovery()
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BananaGold)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Auto-Scan", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Section 3: Discovered Local ADB Services
                        Text("Discovered Local ADB Services:", color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        val localServices = discoveredDevices.filter { it.ip == "127.0.0.1" || it.ip.startsWith("192.168.") }
                        if (localServices.isNotEmpty()) {
                            localServices.take(2).forEach { dev ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CyberCyan)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text("${dev.ip}:${dev.port}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(dev.name.ifEmpty { "adb-service" }, color = CyberCyan, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.connect(dev.ip, dev.port) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Connect", color = BackgroundDark, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        }

                        // Section 4: Pair Local Device (Automatic via Notification)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Pair Local Device (Automatic via Notification)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("1. Tap 'Start Service' below to enable background pairing listener", color = TextSecondary, fontSize = 12.sp)
                                Text("2. When prompted in notifications, enter the pairing code from Wireless Debugging", color = TextSecondary, fontSize = 12.sp)

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Local pairing listener active", Toast.LENGTH_SHORT).show()
                                        viewModel.startDiscovery()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardHover),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BananaGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Discovery Service", color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
