package com.baner.banadb.ui.screens

import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baner.banadb.R
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
    val context = LocalContext.current
    val localClipboardManager = LocalClipboardManager.current
    val connectionState by viewModel.connectionState.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()

    var activeTopTab by remember { mutableIntStateOf(0) } // 0: Live Control, 1: Terminal
    var toolsExpanded by remember { mutableStateOf(false) }
    var controlMode by remember { mutableIntStateOf(0) } // 0: D-Pad, 1: Touchpad, 2: Numpad
    var powerUtilitiesExpanded by remember { mutableStateOf(false) }
    var textToSend by remember { mutableStateOf("") }

    // Dialog states
    var showClipboardDialog by remember { mutableStateOf(false) }
    var showQuickSetDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showPowerDialog by remember { mutableStateOf(false) }
    var showScreenshotPreviewDialog by remember { mutableStateOf(false) }
    var capturedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Intercept back swipe/press on remote control
    BackHandler {
        showDisconnectConfirmDialog = true
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = stringResource(R.string.remote_title),
                connectionState = connectionState,
                onDisconnect = {
                    viewModel.disconnect()
                    onBack()
                },
                navigationIcon = {
                    IconButton(onClick = { showDisconnectConfirmDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.remote_btn_back), tint = BananaGold)
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.about_title),
                            tint = TextSecondary
                        )
                    }
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
                            Text(stringResource(R.string.disconnect), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liveSelected) BananaGold else Color.Transparent,
                            contentColor = if (liveSelected) BackgroundDark else TextSecondary
                        )
                    ) {
                        Icon(
                            Icons.Default.VideogameAsset,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (liveSelected) BackgroundDark else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.remote_tab_live),
                            color = if (liveSelected) BackgroundDark else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    val termSelected = activeTopTab == 1
                    Button(
                        onClick = {
                            activeTopTab = 1
                            onNavigateToSection("terminal")
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (termSelected) BananaGold else Color.Transparent,
                            contentColor = if (termSelected) BackgroundDark else TextSecondary
                        )
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (termSelected) BackgroundDark else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.remote_tab_terminal),
                            color = if (termSelected) BackgroundDark else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
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
                                    text = stringResource(R.string.remote_feature_tools),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (toolsExpanded) stringResource(R.string.remote_tools_collapse) else stringResource(R.string.remote_feature_tools_sub),
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
                                    label = stringResource(R.string.tool_apps),
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("apps") }

                                FeatureToolItem(
                                    icon = Icons.Default.Folder,
                                    label = stringResource(R.string.tool_files),
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("files") }

                                FeatureToolItem(
                                    icon = Icons.Default.ContentPaste,
                                    label = stringResource(R.string.tool_clipboard),
                                    tint = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                ) { showClipboardDialog = true }

                                FeatureToolItem(
                                    icon = Icons.Default.ScreenshotMonitor,
                                    label = stringResource(R.string.tool_mirror),
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("mirror") }
                            }

                            // Row 2: Quick Set, Tasks, Gallery, Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FeatureToolItem(
                                    icon = Icons.Default.Tune,
                                    label = stringResource(R.string.tool_quick_set),
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { showQuickSetDialog = true }

                                FeatureToolItem(
                                    icon = Icons.Default.Dns,
                                    label = stringResource(R.string.tool_processes),
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("processes") }

                                FeatureToolItem(
                                    icon = Icons.Default.PhotoLibrary,
                                    label = stringResource(R.string.tool_gallery),
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("gallery") }

                                FeatureToolItem(
                                    icon = Icons.Default.Speed,
                                    label = stringResource(R.string.tool_dashboard),
                                    tint = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("dashboard") }
                            }

                            // Row 3: Notify, Scripts, Battery, Screenshot
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FeatureToolItem(
                                    icon = Icons.Default.Notifications,
                                    label = stringResource(R.string.tool_notifications),
                                    tint = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("notifications") }

                                FeatureToolItem(
                                    icon = Icons.Default.Code,
                                    label = stringResource(R.string.tool_scripts),
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) { onNavigateToSection("scripts") }

                                FeatureToolItem(
                                    icon = Icons.Default.BatteryChargingFull,
                                    label = stringResource(R.string.tool_battery),
                                    tint = NeonEmerald,
                                    modifier = Modifier.weight(1f)
                                ) { showBatteryDialog = true }

                                FeatureToolItem(
                                    icon = Icons.Default.CameraAlt,
                                    label = stringResource(R.string.tool_screenshot),
                                    tint = BananaGold,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Toast.makeText(context, context.getString(R.string.toast_capturing_screenshot), Toast.LENGTH_SHORT).show()
                                    viewModel.captureScreenshot { bmp ->
                                        capturedPreviewBitmap = bmp
                                        showScreenshotPreviewDialog = true
                                    }
                                }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.remote_btn_back), tint = BananaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remote_btn_back), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    // Home
                    Button(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_HOME) },
                        modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.remote_btn_home), tint = BananaGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remote_btn_home), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }

                    // Recents
                    Button(
                        onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_APP_SWITCH) },
                        modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CropSquare, contentDescription = stringResource(R.string.remote_btn_recents), tint = BananaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remote_btn_recents), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
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
                    text = { Text(stringResource(R.string.remote_mode_dpad), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                    icon = { Icon(Icons.Default.ControlCamera, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = controlMode == 1,
                    onClick = { controlMode = 1 },
                    text = { Text(stringResource(R.string.remote_mode_touchpad), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = controlMode == 2,
                    onClick = { controlMode = 2 },
                    text = { Text(stringResource(R.string.remote_mode_numpad), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false) },
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
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RemoteBtn(
                                    icon = Icons.Default.VolumeDown,
                                    label = stringResource(R.string.remote_vol_down),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_VOLUME_DOWN)
                                }
                                RemoteBtn(
                                    icon = Icons.Default.VolumeMute,
                                    label = stringResource(R.string.remote_vol_mute),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_VOLUME_MUTE)
                                }
                                RemoteBtn(
                                    icon = Icons.Default.VolumeUp,
                                    label = stringResource(R.string.remote_vol_up),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_VOLUME_UP)
                                }
                            }
                        }

                        // Quick Tools row: Search, Notify, Screenshot
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RemoteBtn(
                                    icon = Icons.Default.Search,
                                    label = stringResource(R.string.remote_search),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.sendKeyevent(KeyEvent.KEYCODE_SEARCH)
                                }
                                RemoteBtn(
                                    icon = Icons.Default.Notifications,
                                    label = stringResource(R.string.remote_notify),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    viewModel.executeCommand("cmd statusbar expand-notifications")
                                }
                                RemoteBtn(
                                    icon = Icons.Default.CameraAlt,
                                    label = stringResource(R.string.remote_screenshot),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Toast.makeText(context, context.getString(R.string.toast_capturing_screenshot), Toast.LENGTH_SHORT).show()
                                    viewModel.captureScreenshot { bmp ->
                                        capturedPreviewBitmap = bmp
                                        showScreenshotPreviewDialog = true
                                    }
                                }
                            }
                        }

                        // Power Utilities Card (Expandable)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { powerUtilitiesExpanded = !powerUtilitiesExpanded }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Power Utilities",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.remote_power_utilities),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.remote_power_utilities_sub),
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = if (powerUtilitiesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (powerUtilitiesExpanded) "Collapse Power Utilities" else "Expand Power Utilities",
                                        tint = CyberCyan
                                    )
                                }

                                AnimatedVisibility(visible = powerUtilitiesExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            color = SurfaceCardHover
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Row 1: Wake, Lock, Power, Bright+, Bright-
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RemoteBtn(
                                                icon = Icons.Default.WbSunny,
                                                label = stringResource(R.string.remote_wake),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 224")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.Lock,
                                                label = stringResource(R.string.remote_lock),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 223")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.PowerSettingsNew,
                                                label = stringResource(R.string.remote_power),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 26")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.Brightness6,
                                                label = stringResource(R.string.remote_bright_plus),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 221")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.Brightness5,
                                                label = stringResource(R.string.remote_bright_minus),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 220")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Row 2: Settings, Voice, Cut, Redo, Undo
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RemoteBtn(
                                                icon = Icons.Default.Settings,
                                                label = stringResource(R.string.remote_settings),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("am start -a android.settings.SETTINGS")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.Mic,
                                                label = stringResource(R.string.remote_voice),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 231")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.ContentCut,
                                                label = stringResource(R.string.remote_cut),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 276")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.Redo,
                                                label = stringResource(R.string.remote_redo),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 283")
                                            }
                                            RemoteBtn(
                                                icon = Icons.Default.Undo,
                                                label = stringResource(R.string.remote_undo),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                viewModel.executeCommand("input keyevent 284")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Keyboard section header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Keyboard",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.remote_keyboard),
                                color = CyberCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Text Injection Field with trailing send button
                        OutlinedTextField(
                            value = textToSend,
                            onValueChange = { textToSend = it },
                            placeholder = { Text(stringResource(R.string.remote_keyboard_placeholder), color = TextSecondary, fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (textToSend.isNotEmpty()) {
                                        viewModel.sendText(textToSend)
                                        textToSend = ""
                                    }
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = CyberCyan
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = SurfaceCardHover,
                                focusedLabelColor = CyberCyan
                            )
                        )

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
                                Text(stringResource(R.string.remote_special_keys), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                // Row 1
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KeyButton(stringResource(R.string.remote_key_enter), icon = Icons.Default.KeyboardReturn, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_ENTER) }
                                    KeyButton(stringResource(R.string.remote_key_del), icon = Icons.Default.Backspace, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_DEL) }
                                    KeyButton(stringResource(R.string.remote_key_tab), icon = Icons.Default.KeyboardTab, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_TAB) }
                                    KeyButton(stringResource(R.string.remote_key_space), icon = Icons.Default.SpaceBar, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_SPACE) }
                                }
                                // Row 2
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KeyButton(stringResource(R.string.remote_key_esc), icon = Icons.Default.Cancel, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_ESCAPE) }
                                    KeyButton(stringResource(R.string.remote_key_copy), icon = Icons.Default.ContentCopy, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_COPY) }
                                    KeyButton(stringResource(R.string.remote_key_paste), icon = Icons.Default.ContentPaste, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_PASTE) }
                                    KeyButton(stringResource(R.string.remote_key_all), icon = Icons.Default.SelectAll, modifier = Modifier.weight(1f)) {
                                        viewModel.executeCommand("input keyevent --meta 113 29") // Ctrl+A
                                    }
                                }
                                // Row 3
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KeyButton(stringResource(R.string.remote_key_home), icon = Icons.Default.VerticalAlignTop, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_MOVE_HOME) }
                                    KeyButton(stringResource(R.string.remote_key_end), icon = Icons.Default.VerticalAlignBottom, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_MOVE_END) }
                                    KeyButton(stringResource(R.string.remote_key_pgup), icon = Icons.Default.KeyboardArrowUp, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_PAGE_UP) }
                                    KeyButton(stringResource(R.string.remote_key_pgdn), icon = Icons.Default.KeyboardArrowDown, modifier = Modifier.weight(1f)) { viewModel.sendKeyevent(KeyEvent.KEYCODE_PAGE_DOWN) }
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
                                Icon(Icons.Default.Backspace, contentDescription = stringResource(R.string.delete), tint = StatusError, modifier = Modifier.size(20.dp))
                            }

                            Button(
                                onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_CALL) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = stringResource(R.string.call), tint = NeonEmerald, modifier = Modifier.size(22.dp))
                            }

                            Button(
                                onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_ENTER) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                            ) {
                                Icon(Icons.Default.KeyboardReturn, contentDescription = stringResource(R.string.remote_key_enter), tint = BackgroundDark, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    // Dialog: Remote & Local Clipboard Manager
    if (showClipboardDialog) {
        var remoteClip by remember { mutableStateOf("") }
        var inputClip by remember { mutableStateOf("") }
        var isFetching by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isFetching = true
            viewModel.syncClipboardFromRemote { text ->
                remoteClip = text
                isFetching = false
            }
        }

        AlertDialog(
            onDismissRequest = { showClipboardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = BananaGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.remote_clipboard_title), color = BananaGold, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Remote clipboard display card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.remote_clipboard_device), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                if (isFetching) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BananaGold)
                                } else {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                isFetching = true
                                                viewModel.syncClipboardFromRemote { text ->
                                                    remoteClip = text
                                                    isFetching = false
                                                    Toast.makeText(context, context.getString(R.string.toast_clipboard_updated), Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh), tint = BananaGold, modifier = Modifier.size(16.dp))
                                        }
                                        if (remoteClip.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    localClipboardManager.setText(AnnotatedString(remoteClip))
                                                    Toast.makeText(context, context.getString(R.string.toast_copied_to_phone), Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy), tint = CyberCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = remoteClip.ifEmpty { stringResource(R.string.remote_clipboard_empty_or_protected) },
                                color = if (remoteClip.isEmpty()) TextMuted else TextPrimary,
                                fontSize = 12.sp,
                                maxLines = 4
                            )
                        }
                    }

                    // Phone Clipboard Quick Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val phoneText = localClipboardManager.getText()?.text ?: ""
                                if (phoneText.isNotEmpty()) {
                                    inputClip = phoneText
                                    Toast.makeText(context, context.getString(R.string.toast_pasted_from_phone), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.toast_phone_clipboard_empty), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BananaGold),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentPasteGo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.remote_clipboard_paste), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { inputClip = "" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.clear), fontSize = 11.sp)
                        }
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = inputClip,
                        onValueChange = { inputClip = it },
                        label = { Text(stringResource(R.string.remote_clipboard_send_label), fontSize = 12.sp) },
                        placeholder = { Text(stringResource(R.string.remote_clipboard_send_hint), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BananaGold,
                            focusedLabelColor = BananaGold,
                            unfocusedBorderColor = SurfaceCardHover
                        )
                    )

                    // Explanation note
                    Text(
                        text = stringResource(R.string.remote_clipboard_tip),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direct Inject button (input text)
                    Button(
                        onClick = {
                            if (inputClip.isNotBlank()) {
                                viewModel.sendText(inputClip)
                                Toast.makeText(context, context.getString(R.string.toast_text_injected), Toast.LENGTH_SHORT).show()
                                showClipboardDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.KeyboardAlt, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remote_clipboard_inject), color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Set target clipboard
                    Button(
                        onClick = {
                            if (inputClip.isNotBlank()) {
                                viewModel.syncClipboardToRemote(inputClip, injectInput = false) { success ->
                                    Toast.makeText(context, if (success) context.getString(R.string.toast_remote_clipboard_set) else context.getString(R.string.toast_remote_clipboard_failed), Toast.LENGTH_SHORT).show()
                                }
                                showClipboardDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remote_clipboard_set), color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClipboardDialog = false }) {
                    Text(stringResource(R.string.close), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog: Quick Settings
    if (showQuickSetDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSetDialog = false },
            title = { Text(stringResource(R.string.remote_quick_settings_title), color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_qs_open_settings), color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null, tint = BananaGold) },
                        modifier = Modifier.clickable {
                            viewModel.executeCommand("am start -a android.settings.SETTINGS")
                            showQuickSetDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_qs_expand_panel), color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Tune, contentDescription = null, tint = CyberCyan) },
                        modifier = Modifier.clickable {
                            viewModel.executeCommand("cmd statusbar expand-settings")
                            showQuickSetDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_qs_wifi_on), color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonEmerald) },
                        modifier = Modifier.clickable {
                            viewModel.toggleWifi(true)
                            showQuickSetDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_qs_bt_on), color = TextPrimary) },
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
                    Text(stringResource(R.string.close), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Battery Info
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text(stringResource(R.string.remote_battery_title), color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.remote_battery_level, dashboardData.batteryLevel), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.remote_battery_status, dashboardData.batteryStatus), color = TextSecondary)
                    Text(stringResource(R.string.remote_battery_temp, dashboardData.batteryTemp.toString()), color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBatteryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                ) {
                    Text(stringResource(R.string.ok), color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Power Actions
    if (showPowerDialog) {
        AlertDialog(
            onDismissRequest = { showPowerDialog = false },
            title = { Text(stringResource(R.string.remote_power_menu_title), color = StatusError, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_power_screen_off), color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = StatusError) },
                        modifier = Modifier.clickable {
                            viewModel.sendKeyevent(KeyEvent.KEYCODE_POWER)
                            showPowerDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_power_reboot), color = StatusError) },
                        leadingContent = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = StatusError) },
                        modifier = Modifier.clickable {
                            viewModel.rebootDevice()
                            showPowerDialog = false
                            onBack()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_power_bootloader), color = TextPrimary) },
                        leadingContent = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = BananaGold) },
                        modifier = Modifier.clickable {
                            viewModel.rebootDevice("bootloader")
                            showPowerDialog = false
                            onBack()
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.remote_power_recovery), color = TextPrimary) },
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
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Screenshot Preview & Save to Gallery
    if (showScreenshotPreviewDialog && capturedPreviewBitmap != null) {
        Dialog(
            onDismissRequest = { showScreenshotPreviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.screenshot_captured_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BananaGold
                        )
                        IconButton(onClick = { showScreenshotPreviewDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedPreviewBitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.toast_retaking_screenshot), Toast.LENGTH_SHORT).show()
                                viewModel.captureScreenshot { bmp ->
                                    capturedPreviewBitmap = bmp
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BananaGold)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.remote_screenshot_retake), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                capturedPreviewBitmap?.let { bmp ->
                                    viewModel.saveScreenshotToGallery(context, bmp) { path ->
                                        Toast.makeText(context, context.getString(R.string.toast_saved_to_gallery, path), Toast.LENGTH_LONG).show()
                                        showScreenshotPreviewDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.remote_screenshot_save), color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Disconnect Confirmation
    if (showDisconnectConfirmDialog) {
        val fallbackDevice = stringResource(R.string.remote_device_fallback)
        val connName = (connectionState as? ConnectionState.Connected)?.deviceName ?: fallbackDevice
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = StatusError, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = stringResource(R.string.disconnect_dialog_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disconnect_dialog_desc, connName),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirmDialog = false
                        viewModel.disconnect()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.disconnect), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
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
private fun RemoteBtn(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun KeyButton(
    label: String,
    icon: ImageVector? = null,
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
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = BananaGold, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
