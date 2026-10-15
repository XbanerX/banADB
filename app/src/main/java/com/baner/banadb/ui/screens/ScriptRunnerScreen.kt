package com.baner.banadb.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.data.model.ScriptItem
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun ScriptRunnerScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit,
    onNavigateToTerminal: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scripts by viewModel.scripts.collectAsState()

    var showAddScriptDialog by remember { mutableStateOf(false) }
    var scriptTitle by remember { mutableStateOf("") }
    var scriptCommand by remember { mutableStateOf("") }
    var scriptCategory by remember { mutableStateOf("Custom") }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "Automation & Scripts",
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
                    IconButton(onClick = onNavigateToTerminal) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = CyberCyan)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddScriptDialog = true },
                containerColor = BananaGold,
                contentColor = BackgroundDark,
                icon = { Icon(Icons.Default.Add, "Add Script") },
                text = { Text("New Script", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Pre-built & Custom ADB Commands",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scripts, key = { it.id }) { script ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (script.isPreset) CyberCyan.copy(alpha = 0.2f) else BananaGold.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = script.category.uppercase(),
                                            color = if (script.isPreset) CyberCyan else BananaGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = script.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            viewModel.runScript(script)
                                            onNavigateToTerminal()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BananaGold),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    if (!script.isPreset) {
                                        IconButton(onClick = { viewModel.deleteScript(script.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = script.command,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TerminalGreen,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Script Dialog
    if (showAddScriptDialog) {
        AlertDialog(
            onDismissRequest = { showAddScriptDialog = false },
            title = { Text("Create Custom Script") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = scriptTitle,
                        onValueChange = { scriptTitle = it },
                        label = { Text("Script Title") },
                        placeholder = { Text("e.g. Force Dark Mode") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BananaGold, focusedLabelColor = BananaGold)
                    )
                    OutlinedTextField(
                        value = scriptCommand,
                        onValueChange = { scriptCommand = it },
                        label = { Text("ADB Shell Command") },
                        placeholder = { Text("e.g. cmd uimode night yes") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BananaGold, focusedLabelColor = BananaGold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scriptTitle.isNotBlank() && scriptCommand.isNotBlank()) {
                            viewModel.saveScript(scriptTitle.trim(), scriptCommand.trim(), scriptCategory)
                            scriptTitle = ""
                            scriptCommand = ""
                            showAddScriptDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                ) {
                    Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScriptDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
