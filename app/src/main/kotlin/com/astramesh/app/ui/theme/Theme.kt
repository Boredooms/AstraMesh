package com.astramesh.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AstraColorScheme = darkColorScheme(
    primary = AstraAccent,
    onPrimary = AstraBlack,
    background = AstraBlack,
    onBackground = AstraTextPrimary,
    surface = AstraSurface,
    onSurface = AstraTextPrimary,
    surfaceVariant = AstraPanel,
    onSurfaceVariant = AstraTextSecondary,
    outline = AstraBorder,
    error = AstraCritical,
    onError = AstraBlack,
)

private val AstraTypography = Typography()

/**
 * AstraMesh is intentionally dark-only: it is a black-first, calm, technical UI
 * meant to stay readable in low-light / emergency conditions (docs/design.md §3).
 */
@Composable
fun AstraMeshTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AstraColorScheme,
        typography = AstraTypography,
        content = content,
    )
}
