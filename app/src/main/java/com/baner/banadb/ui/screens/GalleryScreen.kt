package com.baner.banadb.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.baner.banadb.data.model.RemoteMediaItem
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mediaItems by viewModel.galleryMedia.collectAsState()
    val isLoading by viewModel.isGalleryLoading.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedMediaItem by remember { mutableStateOf<RemoteMediaItem?>(null) }
    var itemToDelete by remember { mutableStateOf<RemoteMediaItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshGallery()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val filteredList = remember(mediaItems, selectedCategory) {
        if (selectedCategory == "All") mediaItems
        else mediaItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Remote Gallery",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${mediaItems.size} photos & videos",
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
                        onClick = { viewModel.refreshGallery() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan,
                            modifier = if (isLoading) Modifier.rotate(rotation) else Modifier
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

            // Categories Row
            val categories = listOf("All", "Camera", "Screenshots", "Videos", "Downloads", "Pictures")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val count = if (cat == "All") mediaItems.size else mediaItems.count { it.category.equals(cat, ignoreCase = true) }

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

            // Content
            if (isLoading && mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Scanning remote media files...", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else if (filteredList.isEmpty()) {
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
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedCategory != "All") "No $selectedCategory media found on remote phone." else "No media files found on remote phone.",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.path }) { item ->
                        MediaGridCard(
                            item = item,
                            viewModel = viewModel,
                            onClick = { selectedMediaItem = item }
                        )
                    }
                }
            }
        }
    }

    // Media Detail & Preview Dialog
    selectedMediaItem?.let { item ->
        MediaPreviewDialog(
            item = item,
            viewModel = viewModel,
            onDismiss = { selectedMediaItem = null },
            onDeleteRequest = {
                itemToDelete = item
                selectedMediaItem = null
            }
        )
    }

    // Confirm Delete Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Remote Media?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${item.name}\" from the remote phone?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = item
                        itemToDelete = null
                        viewModel.deleteRemoteMedia(toRemove) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "Deleted ${toRemove.name}" else "Failed to delete file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun MediaGridCard(
    item: RemoteMediaItem,
    viewModel: AdbViewModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(item.path, item.size) {
        mutableStateOf<Bitmap?>(viewModel.getCachedThumbnail(item.path, item.size))
    }
    var isLoadingThumb by remember(item.path, item.size) {
        mutableStateOf(thumbnailBitmap == null)
    }

    LaunchedEffect(item.path, item.size) {
        if (thumbnailBitmap == null) {
            val bmp = viewModel.loadThumbnail(context, item)
            thumbnailBitmap = bmp
            isLoadingThumb = false
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCardHover, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // Visual Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (isLoadingThumb) {
                    CircularProgressIndicator(
                        color = CyberCyan.copy(alpha = 0.6f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.PlayCircle else Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = CyberCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Video Tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("VIDEO", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Photo Tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val ext = item.name.substringAfterLast('.').uppercase()
                        Text(ext, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Category pill top-left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.category, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Info Section
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatFileSize(item.size),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatDate(item.timestamp),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaPreviewDialog(
    item: RemoteMediaItem,
    viewModel: AdbViewModel,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    var previewFile by remember { mutableStateOf<File?>(null) }
    var isLoadingPreview by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.path) {
        isLoadingPreview = true
        viewModel.loadPreviewFile(context, item) { file ->
            previewFile = file
            isLoadingPreview = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with title and close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.isVideo) "Video Preview" else "Photo Preview",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BackgroundDark),
                    contentAlignment = Alignment.Center
                ) {
                    val cachedThumb = remember(item.path, item.size) {
                        viewModel.getCachedThumbnail(item.path, item.size)
                    }

                    if (isLoadingPreview) {
                        if (cachedThumb != null) {
                            Image(
                                bitmap = cachedThumb.asImageBitmap(),
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(32.dp))
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Loading preview from remote...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    } else if (previewFile != null && previewFile!!.exists()) {
                        if (item.isVideo) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (cachedThumb != null) {
                                    Image(
                                        bitmap = cachedThumb.asImageBitmap(),
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyan)
                                            .clickable {
                                                openMediaFile(context, previewFile!!, "video/*")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = BackgroundDark,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Tap to Play Video", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        } else {
                            val bitmap = remember(previewFile) {
                                try {
                                    BitmapFactory.decodeFile(previewFile!!.absolutePath)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (cachedThumb != null) {
                                Image(
                                    bitmap = cachedThumb.asImageBitmap(),
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("Cannot render image preview", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        if (cachedThumb != null) {
                            Image(
                                bitmap = cachedThumb.asImageBitmap(),
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Preview unavailable", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File metadata details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Path: ${item.path}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Size: ${formatFileSize(item.size)}", color = TextSecondary, fontSize = 11.sp)
                        Text("Date: ${formatDate(item.timestamp)}", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                statusText?.let { st ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(st, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save to Phone button (Cyan Accent)
                    Button(
                        onClick = {
                            isDownloading = true
                            statusText = "Saving to your phone..."
                            viewModel.downloadMediaToLocal(
                                context = context,
                                item = item,
                                onProgress = { statusText = it },
                                onComplete = { ok, pathOrMsg ->
                                    isDownloading = false
                                    statusText = null
                                    if (ok) {
                                        Toast.makeText(context, "Saved to Pictures/banADB!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed: $pathOrMsg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save",
                            tint = BackgroundDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Phone",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            previewFile?.let { f ->
                                shareMediaFile(context, f, if (item.isVideo) "video/*" else "image/*")
                            } ?: run {
                                Toast.makeText(context, "Loading file first...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = CyberCyan)
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteRequest,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError)
                    }
                }
            }
        }
    }
}

private fun openMediaFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to play media", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMediaFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
