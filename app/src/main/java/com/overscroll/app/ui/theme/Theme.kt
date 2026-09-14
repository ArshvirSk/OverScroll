package com.overscroll.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OverscrollLightColorScheme = lightColorScheme(
    primary = OverscrollPrimary,
    onPrimary = OverscrollOnPrimary,
    primaryContainer = OverscrollPrimaryContainer,
    onPrimaryContainer = OverscrollOnPrimaryContainer,
    secondary = OverscrollSecondary,
    onSecondary = OverscrollOnSecondary,
    tertiary = OverscrollTertiary,
    onTertiary = OverscrollOnTertiary,
    background = OverscrollSurface,
    onBackground = OverscrollOnSurface,
    surface = OverscrollSurface,
    onSurface = OverscrollOnSurface,
    surfaceVariant = OverscrollSurfaceVariant,
    onSurfaceVariant = OverscrollOnSurfaceVariant,
    error = OverscrollError,
    onError = Color.White,
    outline = OverscrollOutline,
    outlineVariant = OverscrollOutlineVariant,
)

@Composable
fun OverscrollTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = OverscrollLightColorScheme,
        typography = OverscrollTypography,
        content = content,
    )
}
