package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryOak,
    secondary = SecondarySage,
    tertiary = TertiaryClay,
    background = CharcoalDarkBg,
    surface = CozyWalnutSurface,
    onPrimary = IvoryWhiteText,
    onSecondary = IvoryWhiteText,
    onBackground = IvoryWhiteText,
    onSurface = IvoryWhiteText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryOak,
    secondary = SecondarySage,
    tertiary = TertiaryClay,
    background = CreamBgLight,
    surface = SandSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF2E241E),
    onSurface = Color(0xFF2E241E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Custom brand styling: set dynamic to false by default for consistency
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
