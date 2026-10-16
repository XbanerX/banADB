package com.baner.banadb.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.baner.banadb.R
import com.baner.banadb.data.model.RemoteFile
import com.baner.banadb.ui.components.BanaTopBar
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel
import java.io.File
import java.util.Locale

enum class FileCategory {
    IMAGE, VIDEO, AUDIO, TEXT, PDF, APK, OTHER
}

fun getFileCategory(name: String): FileCategory {
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif", "bmp" -> FileCategory.IMAGE
        "mp4", "mkv", "webm", "avi", "3gp", "mov" -> FileCategory.VIDEO
        "mp3", "wav", "ogg", "flac", "aac", "m4a" -> FileCategory.AUDIO
        "txt", "log", "json", "xml", "prop", "sh", "rc", "conf", "yaml", "yml", "md", "ini", "csv", "properties" -> FileCategory.TEXT
        "pdf" -> FileCategory.PDF
        "apk" -> FileCategory.APK
        else -> FileCategory.OTHER
    }
}

fun getMimeType(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "3gp" -> "video/3gpp"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/x-wav"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "txt", "log", "prop", "sh", "rc", "conf", "ini", "md" -> "text/plain"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "pdf" -> "application/pdf"
        "apk" -> "application/vnd.android.package-archive"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: AdbViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val currentPath by viewModel.currentRemotePath.collectAsState()
    val files by viewModel.remoteFiles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOperationLoading by viewModel.isOperationLoading.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<RemoteFile?>(null) }
    var selectedFileForOptions by remember { mutableStateOf<RemoteFile?>(null) }

    // Previews
    var previewImageFile by remember { mutableStateOf<Pair<RemoteFile, File>?>(null) }
    var previewTextFile by remember { mutableStateOf<Pair<RemoteFile, String>?>(null) }

    // Back handling inside folders: go to parent directory first
    val canGoUp = currentPath != "/" && currentPath.isNotBlank() && currentPath != "/sdcard"
    BackHandler(enabled = canGoUp) {
        val parent = currentPath.substringBeforeLast('/', "").ifEmpty { "/" }
        viewModel.navigateToPath(parent)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshFiles()
    }

    fun openWithExternalApp(localFile: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun handleFileClick(file: RemoteFile) {
        if (file.isDirectory) {
            viewModel.navigateToPath(file.path)
            return
        }

        val category = getFileCategory(file.name)
        viewModel.pullFileToCache(file, context) { cached ->
            if (cached == null) {
                Toast.makeText(context, "Failed to fetch file from remote device", Toast.LENGTH_SHORT).show()
                return@pullFileToCache
            }

            when (category) {
                FileCategory.IMAGE -> {
                    previewImageFile = Pair(file, cached)
                }
                FileCategory.TEXT -> {
                    try {
                        val text = cached.readText(Charsets.UTF_8).take(100_000)
                        previewTextFile = Pair(file, text)
                    } catch (e: Exception) {
                        openWithExternalApp(cached, getMimeType(file.name))
                    }
                }
                FileCategory.VIDEO, FileCategory.AUDIO, FileCategory.PDF, FileCategory.APK, FileCategory.OTHER -> {
                    openWithExternalApp(cached, getMimeType(file.name))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            BanaTopBar(
                title = stringResource(R.string.files_title),
                connectionState = connectionState,
                onDisconnect = {
                    viewModel.disconnect()
                    onBack()
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canGoUp) {
                            val parent = currentPath.substringBeforeLast('/', "").ifEmpty { "/" }
                            viewModel.navigateToPath(parent)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.remote_btn_back), tint = BananaGold)
                    }
                },
                actions = {
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(R.string.files_new_folder), tint = BananaGold)
                    }
                    IconButton(onClick = { viewModel.refreshFiles() }) {
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
            // Path Navigation Breadcrumb Bar
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val parent = currentPath.substringBeforeLast('/', "").ifEmpty { "/" }
                            viewModel.navigateToPath(parent)
                        },
                        enabled = currentPath != "/" && currentPath.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Parent Directory",
                            tint = if (currentPath != "/") BananaGold else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = currentPath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = BananaGoldLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if ((isRefreshing || isOperationLoading) && files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BananaGold)
                }
            } else if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Directory is empty", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(files, key = { index, file -> "${file.path}_$index" }) { _, file ->
                        val category = if (file.isDirectory) null else getFileCategory(file.name)
                        val iconVector = when {
                            file.isDirectory -> Icons.Default.Folder
                            category == FileCategory.IMAGE -> Icons.Default.Image
                            category == FileCategory.VIDEO -> Icons.Default.Movie
                            category == FileCategory.AUDIO -> Icons.Default.MusicNote
                            category == FileCategory.TEXT -> Icons.Default.Description
                            category == FileCategory.APK -> Icons.Default.Android
                            category == FileCategory.PDF -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.InsertDriveFile
                        }
                        val iconColor = when {
                            file.isDirectory -> BananaGold
                            category == FileCategory.IMAGE -> CyberCyan
                            category == FileCategory.VIDEO -> Color(0xFFFF5252)
                            category == FileCategory.AUDIO -> Color(0xFFE040FB)
                            category == FileCategory.TEXT -> Color(0xFF69F0AE)
                            category == FileCategory.APK -> Color(0xFF40C4FF)
                            category == FileCategory.PDF -> Color(0xFFFF6E40)
                            else -> TextSecondary
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { handleFileClick(file) }
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(iconColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = file.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!file.isDirectory) {
                                                Text(
                                                    text = formatFileSize(file.size),
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = file.modified,
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Row {
                                    if (!file.isDirectory) {
                                        IconButton(onClick = { selectedFileForOptions = file }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                                        }
                                    } else {
                                        IconButton(onClick = { fileToDelete = file }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: File Options
    selectedFileForOptions?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { selectedFileForOptions = null },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(file.name, color = BananaGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(formatFileSize(file.size) + " • " + file.modified, color = TextMuted, fontSize = 12.sp)

                Divider(color = SurfaceCardHover)

                // Option: View / Open
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val f = file
                            selectedFileForOptions = null
                            handleFileClick(f)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = BananaGold)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("View / Open File", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                // Option: Download to Downloads
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val f = file
                            selectedFileForOptions = null
                            viewModel.saveFileToDownloads(f, context) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "Saved to Downloads: ${f.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Save to Downloads", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                // Option: Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val f = file
                            selectedFileForOptions = null
                            fileToDelete = f
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Delete File", color = StatusError, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Full-screen Image Preview Dialog
    previewImageFile?.let { (remoteFile, localFile) ->
        val bmp = remember(localFile) {
            try {
                BitmapFactory.decodeFile(localFile.absolutePath)
            } catch (e: Exception) {
                null
            }
        }

        Dialog(
            onDismissRequest = { previewImageFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .border(1.dp, BananaGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = remoteFile.name,
                            color = BananaGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { previewImageFile = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = remoteFile.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Failed to decode image", color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                openWithExternalApp(localFile, "image/*")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open With", color = TextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.saveFileToDownloads(remoteFile, context) { ok ->
                                    if (ok) {
                                        Toast.makeText(context, "Saved to Downloads: ${remoteFile.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Downloads", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Monospaced Text / Code / Log Viewer Dialog
    previewTextFile?.let { (remoteFile, textContent) ->
        val clipboardManager = LocalClipboardManager.current
        Dialog(
            onDismissRequest = { previewTextFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .border(1.dp, BananaGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(remoteFile.name, color = BananaGold, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                            Text(formatFileSize(remoteFile.size), color = TextMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { previewTextFile = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = textContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(textContent))
                                Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy All", color = TextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.saveFileToDownloads(remoteFile, context) { ok ->
                                    if (ok) {
                                        Toast.makeText(context, "Saved to Downloads: ${remoteFile.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(stringResource(R.string.files_create_new_folder), color = BananaGold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(stringResource(R.string.files_folder_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BananaGold,
                        focusedLabelColor = BananaGold
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BananaGold)
                ) {
                    Text(stringResource(R.string.files_create), color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.files_delete_title), color = BananaGold) },
            text = { Text(stringResource(R.string.files_delete_desc, file.name), color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val safeGroup = (digitGroups - 1).coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, (safeGroup + 1).toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[safeGroup])
}
