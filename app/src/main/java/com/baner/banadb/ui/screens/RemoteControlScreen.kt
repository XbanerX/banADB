package com.baner.banadb.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
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
import com.baner.banadb.ui.components.DirectionalPad
import com.baner.banadb.ui.components.VirtualTrackpad
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun RemoteControlScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    var controlMode by remember { mutableIntStateOf(0) } // 0: D-Pad, 1: Touchpad, 2: Numpad / Text
    var textToSend by remember { mutableStateOf("") }

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
                }
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Selector Tabs
            TabRow(
                selectedTabIndex = controlMode,
                containerColor = SurfaceCard,
                contentColor = BananaGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
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
                    text = { Text("Trackpad", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = controlMode == 2,
                    onClick = { controlMode = 2 },
                    text = { Text("Numpad / Text", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Control Surface
            when (controlMode) {
                0 -> { // D-Pad Mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
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
                }
                1 -> { // Touchpad Mode
                    VirtualTrackpad(
                        remoteWidth = dashboardData.screenWidth,
                        remoteHeight = dashboardData.screenHeight,
                        onTap = { x, y -> viewModel.sendTap(x, y) },
                        onSwipe = { x1, y1, x2, y2, dur -> viewModel.sendSwipe(x1, y1, x2, y2, dur) }
                    )
                }
                2 -> { // Numpad & Text Injection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Text Injection Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = textToSend,
                                onValueChange = { textToSend = it },
                                placeholder = { Text("Type text to send remotely...") },
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
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Send", color = BackgroundDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Numeric 3x4 Grid
                        val numpadRows = listOf(
                            listOf("1" to KeyEvent.KEYCODE_1, "2" to KeyEvent.KEYCODE_2, "3" to KeyEvent.KEYCODE_3),
                            listOf("4" to KeyEvent.KEYCODE_4, "5" to KeyEvent.KEYCODE_5, "6" to KeyEvent.KEYCODE_6),
                            listOf("7" to KeyEvent.KEYCODE_7, "8" to KeyEvent.KEYCODE_8, "9" to KeyEvent.KEYCODE_9),
                            listOf("*" to KeyEvent.KEYCODE_STAR, "0" to KeyEvent.KEYCODE_0, "#" to KeyEvent.KEYCODE_POUND)
                        )

                        for (row in numpadRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for ((label, keyCode) in row) {
                                    Button(
                                        onClick = { viewModel.sendKeyevent(keyCode) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                                    ) {
                                        Text(label, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Media & Volume Keys
            Card(
                shape = RoundedCornerShape(16.dp),
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
                    ControlButton(icon = Icons.Default.PowerSettingsNew, desc = "Power", tint = StatusError) {
                        viewModel.sendKeyevent(KeyEvent.KEYCODE_POWER)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Bar Controls (Android System Bar)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BananaGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    IconButton(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_BACK) },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BananaGold, modifier = Modifier.size(28.dp))
                    }

                    // Home
                    IconButton(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_HOME) },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = BananaGold, modifier = Modifier.size(30.dp))
                    }

                    // Recents / App Switch
                    IconButton(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_APP_SWITCH) },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(Icons.Default.CropSquare, contentDescription = "Recents", tint = BananaGold, modifier = Modifier.size(26.dp))
                    }

                    // Notifications shade
                    IconButton(
                        onClick = { viewModel.executeCommand("cmd statusbar expand-notifications") },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = CyberCyan, modifier = Modifier.size(24.dp))
                    }
                }
            }
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
