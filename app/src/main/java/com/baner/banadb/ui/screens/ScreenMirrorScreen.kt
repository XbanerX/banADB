package com.baner.banadb.ui.screens

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.R
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ScreenMirrorScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val screenshot by viewModel.screenshotBitmap.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var autoRefresh by remember { mutableStateOf(false) }
    var invertGestures by remember { mutableStateOf(false) }

    val safeWidth = if (dashboardData.screenWidth > 0) dashboardData.screenWidth else 1080
    val safeHeight = if (dashboardData.screenHeight > 0) dashboardData.screenHeight else 2400

    LaunchedEffect(Unit) {
        viewModel.captureScreenshot()
    }

    // Auto-refresh loop when enabled
    LaunchedEffect(autoRefresh) {
        while (autoRefresh && isActive) {
            viewModel.captureScreenshot()
            delay(1200) // 1.2s intervals to balance adb screencap overhead
        }
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = stringResource(R.string.mirror_title),
                connectionState = connectionState,
                onDisconnect = {
                    viewModel.disconnect()
                    onBack()
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.remote_btn_back), tint = BananaGold)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { invertGestures = !invertGestures }) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = stringResource(R.string.mirror_invert),
                                tint = if (invertGestures) BananaGold else TextMuted
                            )
                        }
                        Text(
                            text = if (autoRefresh) stringResource(R.string.mirror_live) else stringResource(R.string.mirror_manual),
                            color = if (autoRefresh) NeonEmerald else TextMuted,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { autoRefresh = !autoRefresh }) {
                            Icon(
                                imageVector = if (autoRefresh) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Live",
                                tint = if (autoRefresh) NeonEmerald else BananaGold
                            )
                        }
                        IconButton(onClick = { viewModel.captureScreenshot() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.mirror_capture), tint = BananaGold)
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Interactive Display Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, BananaGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .pointerInput(safeWidth, safeHeight) {
                        detectTapGestures { offset ->
                            val rx = (offset.x / size.width) * safeWidth
                            val ry = (offset.y / size.height) * safeHeight
                            viewModel.sendTap(rx, ry)
                        }
                    }
                    .pointerInput(safeWidth, safeHeight, invertGestures) {
                        var startX = 0f
                        var startY = 0f
                        var currentX = 0f
                        var currentY = 0f
                        detectDragGestures(
                            onDragStart = { offset ->
                                startX = (offset.x / size.width) * safeWidth
                                startY = (offset.y / size.height) * safeHeight
                                currentX = startX
                                currentY = startY
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentX = (change.position.x / size.width) * safeWidth
                                currentY = (change.position.y / size.height) * safeHeight
                            },
                            onDragEnd = {
                                val dx = currentX - startX
                                val dy = currentY - startY
                                val dist = kotlin.math.hypot(dx, dy)
                                if (dist > 25f) {
                                    val (sx, sy, ex, ey) = if (invertGestures) {
                                        arrayOf(currentX, currentY, startX, startY)
                                    } else {
                                        arrayOf(startX, startY, currentX, currentY)
                                    }
                                    viewModel.sendSwipe(sx, sy, ex, ey, 220)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                screenshot?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Screen Mirror",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: run {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isRefreshing) {
                            CircularProgressIndicator(color = BananaGold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Capturing remote screen...", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.ScreenShare, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No frame captured yet", color = TextSecondary, fontSize = 13.sp)
                            Text("Tap refresh or live to start", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Remote Navigation Control Bar
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BananaGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_BACK) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BananaGold)
                    }
                    IconButton(onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_HOME) }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = BananaGold)
                    }
                    IconButton(onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_APP_SWITCH) }) {
                        Icon(Icons.Default.CropSquare, contentDescription = "Recents", tint = BananaGold)
                    }
                    IconButton(onClick = { viewModel.sendKeyevent(KeyEvent.KEYCODE_POWER) }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = StatusError)
                    }
                }
            }
        }
    }
}
