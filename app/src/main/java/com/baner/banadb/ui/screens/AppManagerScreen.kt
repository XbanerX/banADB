package com.baner.banadb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.ConnectionState
import com.baner.banadb.data.model.RemoteAppInfo
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var selectedAppForActions by remember { mutableStateOf<RemoteAppInfo?>(null) }
    var showUninstallConfirm by remember { mutableStateOf<RemoteAppInfo?>(null) }

    LaunchedEffect(showSystemApps) {
        viewModel.refreshApps(includeSystem = showSystemApps)
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = "App Manager",
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
                    IconButton(onClick = { viewModel.refreshApps(showSystemApps) }) {
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
            // Search Bar & System Toggle
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or package...") },
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
                    focusedBorderColor = BananaGold,
                    unfocusedBorderColor = SurfaceCardHover
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApps.size} applications found",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Include System", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = { showSystemApps = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BananaGold,
                            checkedTrackColor = BananaGold.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isRefreshing && apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BananaGold)
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching apps found", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredApps, key = { index, app -> "${app.packageName}_$index" }) { _, app ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAppForActions = app }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (app.isSystem) SurfaceCardHover else BananaGold.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.take(1).uppercase(),
                                            color = if (app.isSystem) TextMuted else BananaGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = app.label,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            if (app.isSystem) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(SurfaceDark)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("SYSTEM", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = app.packageName,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.launchApp(app.packageName) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch", tint = BananaGold)
                                    }
                                    IconButton(
                                        onClick = { selectedAppForActions = app },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // App Actions Bottom Sheet
    selectedAppForActions?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppForActions = null },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, color = BananaGold, fontWeight = FontWeight.Bold)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                Spacer(modifier = Modifier.height(16.dp))

                ListItem(
                    headlineContent = { Text("Launch Application", color = TextPrimary) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BananaGold) },
                    modifier = Modifier.clickable {
                        viewModel.launchApp(app.packageName)
                        selectedAppForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Force Stop", color = TextPrimary) },
                    leadingContent = { Icon(Icons.Default.Stop, contentDescription = null, tint = StatusPairing) },
                    modifier = Modifier.clickable {
                        viewModel.forceStopApp(app.packageName)
                        selectedAppForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Clear Application Data", color = TextPrimary) },
                    leadingContent = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CyberCyan) },
                    modifier = Modifier.clickable {
                        viewModel.clearAppData(app.packageName)
                        selectedAppForActions = null
                    }
                )
                if (!app.isSystem) {
                    ListItem(
                        headlineContent = { Text("Uninstall Application", color = StatusError) },
                        leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError) },
                        modifier = Modifier.clickable {
                            showUninstallConfirm = app
                            selectedAppForActions = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Uninstall Confirmation Dialog
    showUninstallConfirm?.let { app ->
        AlertDialog(
            onDismissRequest = { showUninstallConfirm = null },
            title = { Text("Uninstall ${app.label}?") },
            text = { Text("This will completely remove ${app.packageName} from the remote device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uninstallApp(app.packageName)
                        showUninstallConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Uninstall", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallConfirm = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
