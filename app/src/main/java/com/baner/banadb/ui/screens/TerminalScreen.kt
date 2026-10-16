package com.baner.banadb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.baner.banadb.R
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun TerminalScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val consoleOutput by viewModel.consoleOutput.collectAsState()
    var commandInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val quickCommands = listOf(
        "pm list packages -3",
        "dumpsys battery",
        "getprop ro.product.model",
        "ip addr",
        "df -h /sdcard",
        "uptime",
        "top -n 1 -m 5",
        "logcat -d -t 25",
        "wm size",
        "svc wifi enable",
        "svc wifi disable"
    )

    // Auto scroll down when output changes
    LaunchedEffect(consoleOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = stringResource(R.string.terminal_title),
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
                    IconButton(onClick = { viewModel.clearConsole() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear), tint = TextSecondary)
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
                .padding(12.dp)
        ) {
            // Suggestion Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardHover),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        TextButton(
                            onClick = {
                                commandInput = cmd
                                viewModel.executeCommand(cmd)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(cmd, color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Terminal Console Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, SurfaceCardHover, RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = consoleOutput,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TerminalGreen,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Command Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = { Text(stringResource(R.string.terminal_placeholder), fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandInput.isNotBlank()) {
                                viewModel.executeCommand(commandInput.trim())
                                commandInput = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BananaGold,
                        unfocusedBorderColor = SurfaceCardHover,
                        focusedLabelColor = BananaGold
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            viewModel.executeCommand(commandInput.trim())
                            commandInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BananaGold)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = BackgroundDark
                    )
                }
            }
        }
    }
}
