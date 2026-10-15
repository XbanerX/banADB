package com.baner.banadb.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.components.DirectionalPad
import com.baner.banadb.ui.components.VirtualTrackpad
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit,
    onNavigateToSection: (String) -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()

    var activeTopTab by remember { mutableIntStateOf(0) } // 0: Live Control, 1: Terminal
    var toolsExpanded by remember { mutableStateOf(false) }
    var controlMode by remember { mutableIntStateOf(0) } // 0: D-Pad, 1: Touchpad, 2: Numpad
    var textToSend by remember { mutableStateOf("") }

    // Dialog states
    var showClipboardDialog by remember { mutableStateOf(false) }
    var showQuickSetDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showPowerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "Remote Control",
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
                    if (connectionState is ConnectionState.Connected) {
                        Button(
                            onClick = {
                                viewModel.disconnect()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connected device subtitle banner
            if (connectionState is ConnectionState.Connected) {
                val conn = connectionState as ConnectionState.Connected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusConnected)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = conn.deviceName,
                            color = BananaGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${conn.ip}:${conn.port}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Top Pill Segment: [ Live Control ] <-> [ Terminal ]
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val liveSelected = activeTopTab == 0
                    Button(
                        onClick = { activeTopTab = 0 },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liveSelected) BananaGold else Color.Transparent,
                            contentColor = if (liveSelected) BackgroundDark else TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.VideogameAsset, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Control", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    val termSelected = activeTopTab == 1
                    Button(
                        onClick = {
                            activeTopTab = 1
                            onNavigateToSection("terminal")
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (termSelected) BananaGold else Color.Transparent,
                            contentColor = if (termSelected) BackgroundDark else TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terminal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Expandable Card: Remote Feature Tools
            val chevronRotation by animateFloatAsState(
                targetValue = if (toolsExpanded) 180f else 0f,
                label = "chevronRotation"
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BananaGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toolsExpanded = !toolsExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BananaGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = BananaGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Remote Feature Tools",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (toolsExpanded) "Tap to collapse tools grid" else "Apps, Files, Mirror, Quick Set & more",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = { toolsExpanded = !toolsExpanded }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = BananaGold,
                                modifier = Modifier.rotate(chevronRotation)
                            )
                        }
                    }

                    // Collapsible Grid of Tools (Matching Screenshot 2)
                    AnimatedVisibility(visible = toolsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: App Mgr, Files, Clipboard, Mirror
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FeatureToolItem(
                                    icon = Icons.Default.Apps,
                                    label = "App Mgr",
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("apps") }

                                FeatureToolItem(
                                    icon = Icons.Default.Folder,
                                    label = "Files",
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("files") }

                                FeatureToolItem(
                                    icon = Icons.Default.ContentPaste,
                                    label = "Clipboard",
                                    tint = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                ) { showClipboardDialog = true }

                                FeatureToolItem(
                                    icon = Icons.Default.ScreenshotMonitor,
                                    label = "Mirror",
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("mirror") }
                            }

                            // Row 2: Quick Set, Tasks, Status, Notify
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FeatureToolItem(
                                    icon = Icons.Default.Tune,
                                    label = "Quick Set",
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { showQuickSetDialog = true }

                                FeatureToolItem(
                                    icon = Icons.Default.Dns,
                                    label = "Tasks",
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("processes") }

                                FeatureToolItem(
                                    icon = Icons.Default.Speed,
                                    label = "Status",
                                    tint = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("dashboard") }

                                FeatureToolItem(
                                    icon = Icons.Default.Notifications,
                                    label = "Notify",
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { viewModel.executeCommand("cmd statusbar expand-notifications") }
                            }

                            // Row 3: Scripts, Battery, Screenshot, Terminal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FeatureToolItem(
                                    icon = Icons.Default.Code,
                                    label = "Scripts",
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("scripts") }

                                FeatureToolItem(
                                    icon = Icons.Default.BatteryChargingFull,
                                    label = "Battery",
                                    tint = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                ) { showBatteryDialog = true }

                                FeatureToolItem(
                                    icon = Icons.Default.CameraAlt,
                                    label = "Screenshot",
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.captureScreenshot()
                                    onNavigateToSection("mirror")
                                }

                                FeatureToolItem(
                                    icon = Icons.Default.Terminal,
                                    label = "Terminal",
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("terminal") }
                            }
                        }
                    }
                }
            }

            // Android System Navigation Bar (Back, Home, Recents)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BananaGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    Button(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_BACK) },
                        modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BananaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    // Home
                    Button(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_HOME) },
                        modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = BananaGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Home", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    // Recents
                    Button(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_APP_SWITCH) },
                        modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CropSquare, contentDescription = "Recents", tint = BananaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recents", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            // Mode Selector Tabs: [ D-Pad ] | [ Touchpad ] | [ Numpad ]
            TabRow(
                selectedTabIndex = controlMode,
                containerColor = SurfaceCard,
                contentColor = BananaGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = controlMode == 0,
                    onClick = { controlMode = 0 },
                    text = { Text("D-Pad", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ControlCamera, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = controlMode == 1,
                    onClick = { controlMode = 1 },
                    text = { Text("Touchpad", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = controlMode == 2,
                    onClick = { controlMode = 2 },
                    text = { Text("Numpad", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Mode Content
            when (controlMode) {
                0 -> { // D-Pad Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Directional Pad
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DirectionalPad(
                                onUp = { viewModel.sendKeyevent(KeyEvent.KEYCODE_DPAD_UP) },
                                onDown = { viewModel.sendKeyevent(KeyEvent.KEYCODE_DPAD_DOWN) },
                                onLeft = { viewModel.sendKeyevent(KeyEvent.KEYCODE_DPAD_LEFT) },
                                onRight = { viewModel.sendKeyevent(KeyEvent.KEYCODE_DPAD_RIGHT) },
                                onOk = { viewModel.sendKeyevent(KeyEvent.KEYCODE_DPAD_CENTER) }
                            )
                        }

                        // Media & Volume row
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ControlButton(icon = Icons.Default.VolumeDown, desc = "Vol -") {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_VOLUME_DOWN)
                                }
                                ControlButton(icon = Icons.Default.VolumeMute, desc = "Mute") {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_VOLUME_MUTE)
                                }
                                ControlButton(icon = Icons.Default.VolumeUp, desc = "Vol +") {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_VOLUME_UP)
                                }
                                ControlButton(icon = Icons.Default.PlayArrow, desc = "Play/Pause") {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                }
                            }
                        }

                        // Quick Tools row: Search, Notify, Screenshot, Power
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ControlButton(icon = Icons.Default.Search, desc = "Search") {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_SEARCH)
                                }
                                ControlButton(icon = Icons.Default.Notifications, desc = "Notify", tint = CyberCyan) {
                                    viewModel.executeCommand("cmd statusbar expand-notifications")
                                }
                                ControlButton(icon = Icons.Default.CameraAlt, desc = "Screenshot", tint = BananaGold) {
                                    viewModel.captureScreenshot()
                                }
                                ControlButton(icon = Icons.Default.PowerSettingsNew, desc = "Power", tint = StatusError) {
                                    showPowerDialog = true
                                }
                            }
                        }

                        // Text Injection Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = textToSend,
                                onValueChange = { textToSend = it },
                                placeholder = { Text("Type text to send remotely...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BananaGold,
                                    unfocusedBorderColor = SurfaceCardHover,
                                    focusedLabelColor = BananaGold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (textToSend.isNotEmpty()) {
                                        viewModel.sendText(textToSend)
                                        textToSend = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BackgroundDark)
                            }
                        }

                        // Special Keys Grid (Matching Screenshot 3)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Special Keys", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                // Row 1
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KeyButton("Enter", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_ENTER) }
                                    KeyButton("Delete", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_DEL) }
                                    KeyButton("Tab", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_TAB) }
                                    KeyButton("Space", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_SPACE) }
                                }
                                // Row 2
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KeyButton("Esc", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_ESCAPE) }
                                    KeyButton("Copy", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_COPY) }
                                    KeyButton("Paste", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_PASTE) }
                                    KeyButton("Select All", Modifier.weight(1f)) {
                                        viewModel.executeCommand("input keyevent --meta 113 29") // Ctrl+A
                                    }
                                }
                                // Row 3
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KeyButton("Home", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_MOVE_HOME) }
                                    KeyButton("End", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_MOVE_END) }
                                    KeyButton("PgUp", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_PAGE_UP) }
                                    KeyButton("PgDn", Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_PAGE_DOWN) }
                                }
                            }
                        }
                    }
                }
                1 -> { // Touchpad Mode
                    VirtualTrackpad(
                        remoteWidth = dashboardData.screenWidth,
                        remoteHeight = dashboardData.screenHeight,
                        onTap = { x, y -> viewModel.sendTap(x, y) },
                        onSwipe = { x1, y1, x2, y2, dur -> viewModel.sendSwipe(x1, y1, x2, y2, dur) },
                        onBack = { viewModel.sendKeyevent(KeyEvent.KEYCODE_BACK) }
                    )
                }
                2 -> { // Numpad Mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val numpadRows = listOf(
                            listOf("1" to KeyEvent.KEYCODE_1, "2" to KeyEvent.KEYCODE_2, "3" to KeyEvent.KEYCODE_3),
                            listOf("4" to KeyEvent.KEYCODE_4, "5" to KeyEvent.KEYCODE_5, "6" to KeyEvent.KEYCODE_6),
                            listOf("7" to KeyEvent.KEYCODE_7, "8" to KeyEvent.KEYCODE_8, "9" to KeyEvent.KEYCODE_9),
                            listOf("*" to KeyEvent.KEYCODE_STAR, "0" to KeyEvent.KEYCODE_0, "#" to KeyEvent.KEYCODE_POUND)
                        )

                        for (row in numpadRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for ((label, keyCode) in row) {
                                    Button(
                                        onClick = { viewModel.sendKeyevent(keyCode) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                                    ) {
                                        Text(label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Bottom Actions Row: Del, Call, Enter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_DEL) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(20.dp))
                            }

                            Button(
                                onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_CALL) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = NeonEmerald, modifier = Modifier.size(22.dp))
                            }

                            Button(
                                onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_ENTER) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                            ) {
                                Icon(Icons.Default.KeyboardReturn, contentDescription = "Enter", tint = BackgroundDark, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Remote Clipboard Manager
    if (showClipboardDialog) {
        var remoteClip by remember { mutableStateOf("") }
        var inputClip by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            viewModel.syncClipboardFromRemote { text ->
                remoteClip = text
            }
        }

        AlertDialog(
            onDismissRequest = { showClipboardDialog = false },
            title = { Text("Remote Clipboard", color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Device Clipboard Content:", color = TextSecondary, fontSize = 12.sp)
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = remoteClip.ifEmpty { "(Clipboard is empty)" },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputClip,
                        onValueChange = { inputClip = it },
                        label = { Text("Send new text to remote clipboard") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BananaGold,
                            focusedLabelColor = BananaGold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputClip.isNotBlank()) {
                            viewModel.syncClipboardToRemote(inputClip)
                        }
                        showClipboardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                ) {
                    Text("Send", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClipboardDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Quick Settings
    if (showQuickSetDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSetDialog = false },
            title = { Text("Quick Settings", color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Open Settings App", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null, tint = BananaGold) },
                        modifier = Modifier.clickable {
                            viewModel.executeCommand("am start -a android.settings.SETTINGS")
                            showQuickSetDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Expand Quick Settings Panel", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Tune, contentDescription = null, tint = CyberCyan) },
                        modifier = Modifier.clickable {
                            viewModel.executeCommand("cmd statusbar expand-settings")
                            showQuickSetDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Toggle Wi-Fi ON", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonEmerald) },
                        modifier = Modifier.clickable {
                            viewModel.toggleWifi(true)
                            showQuickSetDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Toggle Bluetooth ON", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CyberCyan) },
                        modifier = Modifier.clickable {
                            viewModel.toggleBluetooth(true)
                            showQuickSetDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickSetDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Battery Info
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("Battery Status", color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Level: ${dashboardData.batteryLevel}%", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Status: ${dashboardData.batteryStatus}", color = TextSecondary)
                    Text("Temperature: ${dashboardData.batteryTemp}°C", color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBatteryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                ) {
                    Text("OK", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Power Actions
    if (showPowerDialog) {
        AlertDialog(
            onDismissRequest = { showPowerDialog = false },
            title = { Text("Power Utilities", color = StatusError, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Screen Off / Power Button", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = StatusError) },
                        modifier = Modifier.clickable {
                            viewModel.sendKeyevent(KeyEvent.KEYCODE_POWER)
                            showPowerDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Reboot Device", color = StatusError) },
                        leadingContent = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = StatusError) },
                        modifier = Modifier.clickable {
                            viewModel.rebootDevice()
                            showPowerDialog = false
                            onBack()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Reboot to Bootloader", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = BananaGold) },
                        modifier = Modifier.clickable {
                            viewModel.rebootDevice("bootloader")
                            showPowerDialog = false
                            onBack()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Reboot to Recovery", color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Build, contentDescription = null, tint = CyberCyan) },
                        modifier = Modifier.clickable {
                            viewModel.rebootDevice("recovery")
                            showPowerDialog = false
                            onBack()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPowerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun FeatureToolItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    desc: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(SurfaceDark)
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
