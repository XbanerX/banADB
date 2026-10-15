package com.baner.banadb.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanaTopBar(
    title: String,
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = BananaGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Status pill
                val (statusColor, statusText) = when (connectionState) {
                    is ConnectionState.Connected -> StatusConnected to "Connected"
                    is ConnectionState.Connecting -> StatusPairing to "Connecting"
                    is ConnectionState.Pairing -> StatusPairing to "Pairing"
                    is ConnectionState.Error -> StatusError to "Error"
                    ConnectionState.Disconnected -> StatusDisconnected to "Offline"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = navigationIcon ?: {},
        actions = {
            if (connectionState is ConnectionState.Connected) {
                IconButton(onClick = onDisconnect) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = StatusError
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark
        )
    )
}

@Composable
fun DirectionalPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(240.dp)
            .clip(CircleShape)
            .background(SurfaceCard)
            .border(2.dp, BananaGold.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UP
        IconButton(
            onClick = onUp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(54.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = BananaGold, modifier = Modifier.size(36.dp))
        }

        // DOWN
        IconButton(
            onClick = onDown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(54.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = BananaGold, modifier = Modifier.size(36.dp))
        }

        // LEFT
        IconButton(
            onClick = onLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(54.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = BananaGold, modifier = Modifier.size(36.dp))
        }

        // RIGHT
        IconButton(
            onClick = onRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(54.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = BananaGold, modifier = Modifier.size(36.dp))
        }

        // CENTER OK
        Button(
            onClick = onOk,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
            modifier = Modifier.size(68.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("OK", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun VirtualTrackpad(
    remoteWidth: Int,
    remoteHeight: Int,
    onTap: (Float, Float) -> Unit,
    onSwipe: (Float, Float, Float, Float, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var gestureInfo by remember { mutableStateOf("Touch or drag to control remote screen") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .pointerInput(remoteWidth, remoteHeight) {
                detectTapGestures { offset ->
                    val rx = (offset.x / size.width) * remoteWidth
                    val ry = (offset.y / size.height) * remoteHeight
                    gestureInfo = "Tap at (${rx.toInt()}, ${ry.toInt()})"
                    onTap(rx, ry)
                }
            }
            .pointerInput(remoteWidth, remoteHeight) {
                var startX = 0f
                var startY = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        startX = (offset.x / size.width) * remoteWidth
                        startY = (offset.y / size.height) * remoteHeight
                    },
                    onDragEnd = {
                        gestureInfo = "Drag gesture completed"
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentX = (change.position.x / size.width) * remoteWidth
                        val currentY = (change.position.y / size.height) * remoteHeight
                        onSwipe(startX, startY, currentX, currentY, 150)
                        startX = currentX
                        startY = currentY
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Trackpad",
                tint = CyberCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = gestureInfo,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PinInputBoxes(
    pinLength: Int = 6,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Box(contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { focusRequester.requestFocus() }
        ) {
            for (i in 0 until pinLength) {
                val char = pin.getOrNull(i)?.toString() ?: ""
                val isSelected = pin.length == i
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BananaGold else TextMuted.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Hidden input
        TextField(
            value = pin,
            onValueChange = {
                if (it.length <= pinLength && it.all { c -> c.isDigit() }) {
                    pin = it
                    if (it.length == pinLength) {
                        onPinComplete(it)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.Transparent
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
