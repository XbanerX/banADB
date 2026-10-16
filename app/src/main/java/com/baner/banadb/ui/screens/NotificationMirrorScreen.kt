package com.baner.banadb.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baner.banadb.data.model.NotificationItem
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationMirrorScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isAutoSyncEnabled by viewModel.isNotificationSyncEnabled.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        viewModel.refreshNotifications()
    }

    // Refresh rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Filtered notifications
    val filteredList = remember(notifications, searchQuery, selectedCategory) {
        notifications.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.text.contains(searchQuery, ignoreCase = true) ||
                    item.pkg.contains(searchQuery, ignoreCase = true) ||
                    item.subText.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notification Mirror",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${notifications.size} active notifications",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshNotifications() },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan,
                            modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Card: Live Auto-Sync (5s)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Auto-Sync (5s)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Real-time mirror from remote target phone",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { viewModel.toggleNotificationSync(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search notifications, apps, titles...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = SurfaceCardHover,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Row
            val categories = listOf("All", "Messages", "Social", "Email", "System")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val count = if (cat == "All") notifications.size else notifications.count { it.category.equals(cat, ignoreCase = true) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyberCyan else SurfaceCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberCyan else SurfaceCardHover,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (count > 0) "$cat ($count)" else cat,
                            color = if (isSelected) BackgroundDark else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategory != "All") {
                                "No notifications match your filters."
                            } else {
                                "No active notifications on remote phone."
                            },
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id.ifEmpty { "${it.pkg}_${it.title}_${it.timestampMs}" } }) { item ->
                        NotificationCard(
                            item = item,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val fullText = buildString {
                                    if (item.title.isNotEmpty()) appendLine(item.title)
                                    if (item.text.isNotEmpty()) append(item.text)
                                }
                                cm.setPrimaryClip(ClipData.newPlainText("Notification", fullText))
                                Toast.makeText(context, "Copied notification text", Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = {
                                viewModel.dismissNotification(item)
                                Toast.makeText(context, "Dismissing notification...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val cleanApp = item.pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardHover, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // App name header & category tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cleanApp.take(1).uppercase(),
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = cleanApp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Category badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (item.postTime.isNotEmpty()) {
                    Text(
                        text = item.postTime,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            if (item.title.isNotEmpty()) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Body text
            if (item.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.text,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // SubText if present
            if (item.subText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subText,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = CyberCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dismiss",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
