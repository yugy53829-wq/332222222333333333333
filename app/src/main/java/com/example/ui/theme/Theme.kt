package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = SkyCobaltDark,
  onPrimary = Color(0xFF072340),
  primaryContainer = SkyCobaltContainerDark,
  onPrimaryContainer = SkyCobaltOnContainerDark,
  secondary = SoftTealDark,
  onSecondary = Color(0xFF052E2B),
  secondaryContainer = SoftTealContainerDark,
  onSecondaryContainer = SoftTealOnContainerDark,
  background = DarkBg,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceContainer,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorder,
  error = RoseAlertDark,
)

private val LightColorScheme = lightColorScheme(
  primary = CobaltPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = CobaltContainerLight,
  onPrimaryContainer = CobaltOnContainerLight,
  secondary = MineralTealLight,
  onSecondary = Color.White,
  secondaryContainer = MineralTealContainerLight,
  onSecondaryContainer = MineralTealOnContainerLight,
  background = LightBg,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceContainer,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorder,
  outlineVariant = LightBorder,
  error = RoseAlertLight,
)

fun resolveIsDarkTheme(themeMode: String, systemDark: Boolean): Boolean {
  return when (themeMode.uppercase()) {
    "LIGHT" -> false
    "DARK" -> true
    else -> systemDark
  }
}

@Composable
fun SmartKeyClipTheme(
  themeMode: String = "SYSTEM",
  darkTheme: Boolean? = null,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val useDark = darkTheme ?: resolveIsDarkTheme(themeMode, systemDark)

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    useDark -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    content = content,
  )
}
