/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pedronveloso.a11ybutton.model.ThemeMode

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF355F8C),
        onPrimary = Color(0xFFF7F9FC),
        primaryContainer = Color(0xFFD7E3F4),
        onPrimaryContainer = Color(0xFF102A43),
        secondary = Color(0xFF5B6778),
        onSecondary = Color(0xFFF6F7FA),
        secondaryContainer = Color(0xFFDDE3EB),
        onSecondaryContainer = Color(0xFF1B2430),
        tertiary = Color(0xFF5E6E46),
        onTertiary = Color(0xFFF8FAF2),
        tertiaryContainer = Color(0xFFE0E8D2),
        onTertiaryContainer = Color(0xFF233117),
        background = Color(0xFFF5F7FB),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFF5F7FB),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFDFE3EB),
        onSurfaceVariant = Color(0xFF43474E),
        outline = Color(0xFF73777F),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFBF9),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
    )
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFA7C8F0),
        onPrimary = Color(0xFF0C2F4D),
        primaryContainer = Color(0xFF20486C),
        onPrimaryContainer = Color(0xFFD7E3F4),
        secondary = Color(0xFFC1C7D0),
        onSecondary = Color(0xFF2D3138),
        secondaryContainer = Color(0xFF43474E),
        onSecondaryContainer = Color(0xFFDDE3EB),
        tertiary = Color(0xFFC3D0AA),
        onTertiary = Color(0xFF31421F),
        tertiaryContainer = Color(0xFF475A33),
        onTertiaryContainer = Color(0xFFE0E8D2),
        background = Color(0xFF121417),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF121417),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC3C7CF),
        outline = Color(0xFF8D9199),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

@Immutable
data class A11YButtonStatusPalette(
    val positiveContainer: Color,
    val positiveContent: Color,
)

@Composable
fun a11YButtonStatusPalette(): A11YButtonStatusPalette = MaterialTheme.colorScheme.toStatusPalette()

@Composable
fun A11YButtonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
  val darkTheme =
      when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }
  MaterialTheme(
      colorScheme = colorScheme,
      content = content,
  )
}

private fun ColorScheme.toStatusPalette(): A11YButtonStatusPalette =
    A11YButtonStatusPalette(
        positiveContainer = tertiaryContainer,
        positiveContent = onTertiaryContainer,
    )
