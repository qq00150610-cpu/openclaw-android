package com.openclaw.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OcLightColorScheme = lightColorScheme(
    primary = OcPrimary,
    onPrimary = OcOnPrimary,
    primaryContainer = OcPrimaryContainer,
    onPrimaryContainer = OcOnPrimaryContainer,
    secondary = OcSecondary,
    onSecondary = OcOnSecondary,
    secondaryContainer = OcSecondaryContainer,
    onSecondaryContainer = OcOnSecondaryContainer,
    tertiary = OcTertiary,
    onTertiary = OcOnTertiary,
    tertiaryContainer = OcTertiaryContainer,
    onTertiaryContainer = OcOnTertiaryContainer,
    error = OcError,
    onError = OcOnError,
    errorContainer = OcErrorContainer,
    onErrorContainer = OcOnErrorContainer,
    background = OcBackground,
    onBackground = OcOnBackground,
    surface = OcSurface,
    onSurface = OcOnSurface,
    surfaceVariant = OcSurfaceVariant,
    onSurfaceVariant = OcOnSurfaceVariant,
    outline = OcOutline,
    outlineVariant = OcOutlineVariant,
)

private val OcDarkColorScheme = darkColorScheme(
    primary = OcPrimaryDark,
    onPrimary = OcOnPrimaryDark,
    primaryContainer = OcPrimaryContainerDark,
    onPrimaryContainer = OcOnPrimaryContainerDark,
    secondary = OcSecondaryDark,
    onSecondary = OcOnSecondaryDark,
    secondaryContainer = OcSecondaryContainerDark,
    onSecondaryContainer = OcOnSecondaryContainerDark,
    tertiary = OcTertiaryDark,
    onTertiary = OcOnTertiaryDark,
    tertiaryContainer = OcTertiaryContainerDark,
    onTertiaryContainer = OcOnTertiaryContainerDark,
    error = OcErrorDark,
    onError = OcOnErrorDark,
    errorContainer = OcErrorContainerDark,
    onErrorContainer = OcOnErrorContainerDark,
    background = OcBackgroundDark,
    onBackground = OcOnBackgroundDark,
    surface = OcSurfaceDark,
    onSurface = OcOnSurfaceDark,
    surfaceVariant = OcSurfaceVariantDark,
    onSurfaceVariant = OcOnSurfaceVariantDark,
    outline = OcOutlineDark,
    outlineVariant = OcOutlineVariantDark,
)

@Composable
fun OpenClawTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> OcDarkColorScheme
        else -> OcLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OcTypography,
        content = content
    )
}
