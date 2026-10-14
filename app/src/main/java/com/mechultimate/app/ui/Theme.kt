package com.mechultimate.app.ui

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

private val Light = lightColorScheme(
    primary = Color(0xFF0B5CAD), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E3FF), onPrimaryContainer = Color(0xFF001B3C),
    secondary = Color(0xFF00696E), secondaryContainer = Color(0xFFB2F0F2),
    tertiary = Color(0xFFB45309), tertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFFF8FAFC), surface = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFEEF2F7), surfaceContainerHigh = Color(0xFFE6EBF2),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA7C8FF), onPrimary = Color(0xFF003061),
    primaryContainer = Color(0xFF004788), onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFF80D4D9), secondaryContainer = Color(0xFF004F53),
    tertiary = Color(0xFFFFB870), tertiaryContainer = Color(0xFF6B3A00),
    background = Color(0xFF0F1419), surface = Color(0xFF0F1419),
    surfaceContainer = Color(0xFF1B2128), surfaceContainerHigh = Color(0xFF242B33),
)

/** Status colours that stay readable in both themes. */
object StatusColors {
    val pass = Color(0xFF15803D)
    val warn = Color(0xFFB45309)
    val fail = Color(0xFFB91C1C)
    val passDark = Color(0xFF4ADE80)
    val warnDark = Color(0xFFFBBF24)
    val failDark = Color(0xFFF87171)
}

@Composable
fun MechTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
