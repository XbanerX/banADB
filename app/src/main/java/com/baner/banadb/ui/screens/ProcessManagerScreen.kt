package com.baner.banadb.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.baner.banadb.data.model.RunningProcess
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun ProcessManagerScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val processes by viewModel.processes.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var processToKill by remember { mutableStateOf<RunningProcess?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshProcesses()
    }

    val filteredProcesses = remember(processes, searchQuery) {
        if (searchQuery.isBlank()) processes
        else processes.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.pid.contains(searchQuery) ||
            it.user.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "Process Manager",
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
                    IconButton(onClick = { viewModel.refreshProcesses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BananaGold)
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
                .padding(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter processes by name or PID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = SurfaceCardHover
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${filteredProcesses.size} processes running",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isRefreshing && processes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberCyan)
                }
            } else if (filteredProcesses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching processes", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredProcesses, key = { index, proc -> "${proc.pid}_$index" }) { _, proc ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proc.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "PID: ${proc.pid}",
                                            fontFamily = FontFamily.Monospace,
                                            color = BananaGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "USER: ${proc.user}",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                IconButton(onClick = { processToKill = proc }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Kill Process",
                                        tint = StatusError.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Kill confirmation dialog
    processToKill?.let { proc ->
        AlertDialog(
            onDismissRequest = { processToKill = null },
            title = { Text("Kill Process?") },
            text = { Text("Terminate PID ${proc.pid} (${proc.name})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.killProcess(proc.pid)
                        processToKill = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Kill", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { processToKill = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
