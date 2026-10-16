package com.baner.banadb.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.baner.banadb.R
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
                if (title.equals("banADB", ignoreCase = true) || title.equals("BanaADB", ignoreCase = true)) {
                    if (navigationIcon == null) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_banadb_logo),
                            contentDescription = "banADB Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                                append("ban")
                            }
                            withStyle(SpanStyle(color = CyberCyan, fontWeight = FontWeight.ExtraBold)) {
                                append("ADB")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (navigationIcon == null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Status pill
                    val (statusColor, statusText) = when (connectionState) {
                        is ConnectionState.Connected -> StatusConnected to stringResource(R.string.connected)
                        is ConnectionState.Connecting -> StatusPairing to stringResource(R.string.connecting)
                        is ConnectionState.Pairing -> StatusPairing to stringResource(R.string.pairing)
                        is ConnectionState.Error -> StatusError to stringResource(R.string.error)
                        ConnectionState.Disconnected -> StatusDisconnected to stringResource(R.string.offline)
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
                } else {
                    val statusColor = when (connectionState) {
                        is ConnectionState.Connected -> StatusConnected
                        is ConnectionState.Connecting -> StatusPairing
                        is ConnectionState.Pairing -> StatusPairing
                        is ConnectionState.Error -> StatusError
                        ConnectionState.Disconnected -> StatusDisconnected
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }
        },
        navigationIcon = navigationIcon ?: {},
        actions = {
            if (connectionState is ConnectionState.Connected && navigationIcon == null) {
                IconButton(onClick = onDisconnect) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.disconnect),
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
            Text(stringResource(R.string.ok), color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun VirtualTrackpad(
    remoteWidth: Int,
    remoteHeight: Int,
    onTap: (Float, Float) -> Unit,
    onSwipe: (Float, Float, Float, Float, Int) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var gestureInfo by remember { mutableStateOf("Touch or drag to navigate") }
    var invertDirection by remember { mutableStateOf(false) }

    val safeWidth = if (remoteWidth > 0) remoteWidth.toFloat() else 1080f
    val safeHeight = if (remoteHeight > 0) remoteHeight.toFloat() else 2400f

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trackpad header with Invert Scroll Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = BananaGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = gestureInfo,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (invertDirection) "Invert: ON" else "Invert: OFF",
                    color = if (invertDirection) BananaGold else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = invertDirection,
                    onCheckedChange = { invertDirection = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = BananaGold,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceCardHover
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tactile Trackpad Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, BananaGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .pointerInput(safeWidth, safeHeight) {
                    detectTapGestures { offset ->
                        val rx = (offset.x / size.width) * safeWidth
                        val ry = (offset.y / size.height) * safeHeight
                        gestureInfo = "Tap at (${rx.toInt()}, ${ry.toInt()})"
                        onTap(rx, ry)
                    }
                }
                .pointerInput(safeWidth, safeHeight, invertDirection) {
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
                            val distance = kotlin.math.hypot(dx, dy)
                            if (distance > 25f) {
                                val (sx, sy, ex, ey) = if (invertDirection) {
                                    arrayOf(currentX, currentY, startX, startY)
                                } else {
                                    arrayOf(startX, startY, currentX, currentY)
                                }
                                gestureInfo = "Swipe (${(ex - sx).toInt()}, ${(ey - sy).toInt()})"
                                onSwipe(sx, sy, ex, ey, 220)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Trackpad",
                    tint = BananaGold.copy(alpha = 0.4f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Trackpad Touch Surface",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tap to Click • Drag to Swipe",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mouse buttons & Quick Scroll Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Click
            Button(
                onClick = { onTap(safeWidth / 2f, safeHeight / 2f) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = BananaGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tap", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Scroll Up
            IconButton(
                onClick = {
                    val midX = safeWidth / 2f
                    val midY = safeHeight / 2f
                    onSwipe(midX, midY - 300f, midX, midY + 300f, 250)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll Up", tint = BananaGold)
            }

            // Scroll Down
            IconButton(
                onClick = {
                    val midX = safeWidth / 2f
                    val midY = safeHeight / 2f
                    onSwipe(midX, midY + 300f, midX, midY - 300f, 250)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll Down", tint = BananaGold)
            }

            // Right Click / Back
            Button(
                onClick = { onBack?.invoke() ?: onSwipe(50f, safeHeight / 2f, 500f, safeHeight / 2f, 200) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.remote_btn_back), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PinInputBoxes(
    pinLength: Int = 6,
    onPinChange: ((String) -> Unit)? = null,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until pinLength) {
                val char = pin.getOrNull(i)?.toString() ?: ""
                val isSelected = pin.length == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CyberCyan else SurfaceCardHover,
                            shape = RoundedCornerShape(10.dp)
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

        // Hidden input spanning entire row for touch and IME handling
        TextField(
            value = pin,
            onValueChange = {
                if (it.length <= pinLength && it.all { c -> c.isDigit() }) {
                    pin = it
                    onPinChange?.invoke(it)
                    if (it.length == pinLength) {
                        onPinComplete(it)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (pin.length == pinLength) {
                        onPinComplete(pin)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .alpha(0.01f)
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}
