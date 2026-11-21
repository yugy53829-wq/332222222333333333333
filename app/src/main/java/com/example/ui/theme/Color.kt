package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme — Daylight Alabaster & Refined Cobalt-Teal
val LightBg = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFEDF2F7)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

val CobaltPrimaryLight = Color(0xFF0F6CBD)
val CobaltContainerLight = Color(0xFFDCEEFF)
val CobaltOnContainerLight = Color(0xFF072F54)

val MineralTealLight = Color(0xFF0D9488)
val MineralTealContainerLight = Color(0xFFCCFBF1)
val MineralTealOnContainerLight = Color(0xFF0A3D38)

// Dark Theme — Neutral Graphite & Sky Cobalt
val DarkBg = Color(0xFF111318)
val DarkSurface = Color(0xFF1B1F27)
val DarkSurfaceContainer = Color(0xFF242A35)
val DarkBorder = Color(0xFF2E3646)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)

val SkyCobaltDark = Color(0xFF6CB4FF)
val SkyCobaltContainerDark = Color(0xFF0E3962)
val SkyCobaltOnContainerDark = Color(0xFFD6EAFF)

val SoftTealDark = Color(0xFF4FD1C5)
val SoftTealContainerDark = Color(0xFF123D3B)
val SoftTealOnContainerDark = Color(0xFFBAF7F0)

// Semantic Status Tokens
val EmeraldOk = Color(0xFF10B981)
val EmeraldOkDark = Color(0xFF34D399)
val WarmAmber = Color(0xFFD97706)
val WarmAmberDark = Color(0xFFFBBF24)
val AmberContainerLight = Color(0xFFFEF3C7)
val AmberOnContainerLight = Color(0xFF78350F)
val AmberContainerDark = Color(0xFF3A2B0D)
val AmberOnContainerDark = Color(0xFFFDE68A)
val RoseAlertLight = Color(0xFFBE123C)
val RoseAlertDark = Color(0xFFFB7185)

// Dedicated Native Keyboard Palette Tokens (Gboard-caliber contrast & comfort)
data class KeyboardPalette(
  val canvasBg: Color,
  val toolbarBg: Color,
  val keyBg: Color,
  val keyPressedBg: Color,
  val keyLipShadow: Color,
  val actionKeyBg: Color,
  val primaryAccentBg: Color,
  val onPrimaryAccent: Color,
  val toolActiveBg: Color,
  val onToolActive: Color,
  val keyText: Color,
  val keySubText: Color,
  val cardBg: Color,
  val divider: Color,
  val protectedBg: Color,
  val protectedFg: Color,
)

val LightKeyboardPalette = KeyboardPalette(
  canvasBg = Color(0xFFECEFF4),
  toolbarBg = Color(0xFFE4E9F1),
  keyBg = Color(0xFFFFFFFF),
  keyPressedBg = Color(0xFFDCE7F7),
  keyLipShadow = Color(0xFFC4CCD9),
  actionKeyBg = Color(0xFFD5DCE8),
  primaryAccentBg = CobaltPrimaryLight,
  onPrimaryAccent = Color.White,
  toolActiveBg = CobaltContainerLight,
  onToolActive = CobaltPrimaryLight,
  keyText = Color(0xFF111827),
  keySubText = Color(0xFF64748B),
  cardBg = Color(0xFFFFFFFF),
  divider = Color(0xFFD1D9E6),
  protectedBg = AmberContainerLight,
  protectedFg = AmberOnContainerLight,
)

val DarkKeyboardPalette = KeyboardPalette(
  canvasBg = Color(0xFF14171D),
  toolbarBg = Color(0xFF1B1F27),
  keyBg = Color(0xFF2D333F),
  keyPressedBg = Color(0xFF3E4A5E),
  keyLipShadow = Color(0xFF0A0C10),
  actionKeyBg = Color(0xFF222731),
  primaryAccentBg = SkyCobaltDark,
  onPrimaryAccent = Color(0xFF072340),
  toolActiveBg = SkyCobaltContainerDark,
  onToolActive = SkyCobaltDark,
  keyText = Color(0xFFF3F6FA),
  keySubText = Color(0xFF9AA5B8),
  cardBg = Color(0xFF1F242E),
  divider = Color(0xFF2E3544),
  protectedBg = AmberContainerDark,
  protectedFg = AmberOnContainerDark,
)
