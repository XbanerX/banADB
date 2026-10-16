package com.baner.banadb.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.baner.banadb.R
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
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOperationLoading by viewModel.isOperationLoading.collectAsState()
    val operationStatus by viewModel.operationStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var selectedAppForActions by remember { mutableStateOf<RemoteAppInfo?>(null) }
    var showUninstallConfirm by remember { mutableStateOf<RemoteAppInfo?>(null) }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.installApkFromUri(it, context) }
    }

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
                title = stringResource(R.string.apps_title),
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
                    IconButton(onClick = { viewModel.refreshApps(showSystemApps) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BananaGold)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { apkPickerLauncher.launch("*/*") },
                containerColor = BananaGold,
                contentColor = BackgroundDark,
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = BackgroundDark) },
                text = { Text(stringResource(R.string.apps_install_apk), color = BackgroundDark, fontWeight = FontWeight.Bold) }
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
                placeholder = { Text(stringResource(R.string.apps_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = TextMuted)
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
                    text = stringResource(R.string.apps_count, filteredApps.size),
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.apps_include_system), color = TextSecondary, fontSize = 12.sp)
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
                        Text(stringResource(R.string.apps_not_found), color = TextSecondary)
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
                    headlineContent = { Text(stringResource(R.string.apps_launch_action), color = TextPrimary) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BananaGold) },
                    modifier = Modifier.clickable {
                        viewModel.launchApp(app.packageName)
                        selectedAppForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.apps_force_stop), color = TextPrimary) },
                    leadingContent = { Icon(Icons.Default.Stop, contentDescription = null, tint = StatusPairing) },
                    modifier = Modifier.clickable {
                        viewModel.forceStopApp(app.packageName)
                        selectedAppForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.apps_clear_app_data), color = TextPrimary) },
                    leadingContent = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CyberCyan) },
                    modifier = Modifier.clickable {
                        viewModel.clearAppData(app.packageName)
                        selectedAppForActions = null
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.apps_extract_apk), color = TextPrimary) },
                    supportingContent = { Text(stringResource(R.string.apps_extract_desc), color = TextMuted, fontSize = 11.sp) },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null, tint = BananaGold) },
                    modifier = Modifier.clickable {
                        viewModel.extractApk(app.packageName, app.label, context)
                        selectedAppForActions = null
                    }
                )
                if (!app.isSystem) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.apps_uninstall_action), color = StatusError) },
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
            title = { Text(stringResource(R.string.apps_uninstall_title, app.label)) },
            text = { Text(stringResource(R.string.apps_uninstall_desc, app.packageName)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uninstallApp(app.packageName)
                        showUninstallConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(stringResource(R.string.apps_uninstall), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallConfirm = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Operation Loading Dialog
    if (isOperationLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.operation_title), color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(color = BananaGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(operationStatus ?: stringResource(R.string.operation_executing), color = TextPrimary, fontSize = 14.sp)
                }
            },
            confirmButton = {},
            containerColor = SurfaceDark
        )
    }

    // Operation Status Message Dialog
    if (!isOperationLoading && operationStatus != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearOperationStatus() },
            title = { Text(stringResource(R.string.operation_title), color = BananaGold, fontWeight = FontWeight.Bold) },
            text = { Text(operationStatus ?: "", color = TextPrimary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearOperationStatus() },
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                ) {
                    Text(stringResource(R.string.ok), color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
