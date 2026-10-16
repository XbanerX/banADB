package com.baner.banadb.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.baner.banadb.R
import com.baner.banadb.data.repository.DevicePreferences
import com.baner.banadb.ui.theme.*
import com.baner.banadb.ui.viewmodel.AdbViewModel

@Composable
fun AboutDialog(
    viewModel: AdbViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showFullLicense by remember { mutableStateOf(false) }
    val themeMode by (viewModel?.themeMode?.collectAsState() ?: remember { mutableStateOf(DevicePreferences.THEME_SYSTEM) })

    if (showFullLicense) {
        FullLicenseDialog(onDismiss = { showFullLicense = false })
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = TextSecondary)
                    }
                }

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_banadb_logo),
                    contentDescription = "banADB Logo",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // App Brand Name: banADB
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                            append("ban")
                        }
                        withStyle(SpanStyle(color = CyberCyan, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)) {
                            append("ADB")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.app_tagline),
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_version),
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Theme Selector Card
                if (viewModel != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceCardHover, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Palette, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.theme_title), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes: List<Triple<String, String, ImageVector>> = listOf(
                                    Triple(DevicePreferences.THEME_SYSTEM, stringResource(R.string.theme_system), Icons.Default.BrightnessAuto),
                                    Triple(DevicePreferences.THEME_LIGHT, stringResource(R.string.theme_light), Icons.Default.LightMode),
                                    Triple(DevicePreferences.THEME_DARK, stringResource(R.string.theme_dark), Icons.Default.DarkMode)
                                )
                                themes.forEach { (mode, label, icon) ->
                                    val selected = themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) CyberCyan.copy(alpha = 0.2f) else SurfaceDark)
                                            .border(
                                                1.dp,
                                                if (selected) CyberCyan else SurfaceCardHover,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel?.setThemeMode(mode) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (selected) CyberCyan else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                color = if (selected) CyberCyan else TextSecondary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Metadata Info Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardHover, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        AboutInfoRow(
                            icon = Icons.Outlined.Person,
                            label = stringResource(R.string.about_author),
                            value = "baner"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = SurfaceCardHover
                        )

                        AboutInfoRow(
                            icon = Icons.Outlined.Email,
                            label = stringResource(R.string.about_contact),
                            value = "bandroid@banerxus.com",
                            isClickable = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:bandroid@banerxus.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "banADB Feedback")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "bandroid@banerxus.com", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = SurfaceCardHover
                        )

                        AboutInfoRow(
                            icon = Icons.Outlined.Gavel,
                            label = stringResource(R.string.about_license),
                            value = "GNU AGPL v3.0",
                            isClickable = true,
                            onClick = { showFullLicense = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // License Button
                OutlinedButton(
                    onClick = { showFullLicense = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.about_view_license_btn), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Copyright
                Text(
                    text = stringResource(R.string.about_copyright),
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable && onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = value,
                color = if (isClickable) CyberCyan else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (isClickable) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (isClickable) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FullLicenseDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GNU AGPL v3.0",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundDark)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
                            GNU AFFERO GENERAL PUBLIC LICENSE
                            Version 3, 19 November 2007
                            
                            Copyright (C) 2026 baner <bandroid@banerxus.com>
                            
                            This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
                            
                            This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
                            
                            Key Freedoms & Requirements:
                            • Freedom to run the program for any purpose.
                            • Freedom to study how the program works and adapt it.
                            • Freedom to redistribute copies to help others.
                            • Freedom to improve the program and release improvements to the public.
                            • Network copyleft: if modified versions run over a network, corresponding source code must be made available to all network users.
                        """.trimIndent(),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close), color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
