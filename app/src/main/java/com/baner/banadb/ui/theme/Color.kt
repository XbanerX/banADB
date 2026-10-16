package com.baner.banadb.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand Colors - Cyber Cyan
val BrandCyan = Color(0xFF00E5FF)
val BrandCyanLight = Color(0xFF80F3FF)
val BrandCyanDark = Color(0xFF0098B3)
val BrandCyanDeep = Color(0xFF00838F)

// Dark Palette (Cyberpunk / Deep Slate)
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF131A29)
val DarkSurfaceCard = Color(0xFF1B2438)
val DarkSurfaceCardHover = Color(0xFF24304A)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

// Light Palette (Modern Clean Slate / White)
val LightBackground = Color(0xFFF4F6F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightSurfaceCardHover = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)

// Status colors
val StatusConnected = Color(0xFF10B981)
val StatusDisconnected = Color(0xFF64748B)
val StatusError = Color(0xFFEF4444)

// Terminal specific colors (always dark console)
val TerminalBackground = Color(0xFF060911)
val TerminalTextGreen = Color(0xFF34D399)
val TerminalGreen = TerminalTextGreen
val TerminalTextYellow = Color(0xFFFBBF24)
val TerminalTextCyan = Color(0xFF38BDF8)

val NeonEmerald = Color(0xFF10B981)

data class BanaPalette(
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceCardHover: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyberCyan: Color,
    val isDark: Boolean
)

val DarkBanaPalette = BanaPalette(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceCard = DarkSurfaceCard,
    surfaceCardHover = DarkSurfaceCardHover,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    cyberCyan = BrandCyan,
    isDark = true
)

val LightBanaPalette = BanaPalette(
    background = LightBackground,
    surface = LightSurface,
    surfaceCard = LightSurfaceCard,
    surfaceCardHover = LightSurfaceCardHover,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    cyberCyan = BrandCyanDeep,
    isDark = false
)

val LocalBanaPalette = staticCompositionLocalOf { DarkBanaPalette }

// Universal Dynamic Colors (automatically adjust between Light & Dark themes)
val BackgroundDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.background

val SurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.surface

val SurfaceCard: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.surfaceCard

val SurfaceCardHover: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.surfaceCardHover

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.textMuted

val CyberCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.cyberCyan

val CyberCyanLight: Color = BrandCyanLight
val CyberCyanDark: Color = BrandCyanDark

val BananaGold: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.cyberCyan

val BananaGoldLight: Color = BrandCyanLight
val BananaGoldDark: Color = BrandCyanDark

val StatusPairing: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBanaPalette.current.cyberCyan
