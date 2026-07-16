package com.astramesh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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

/** Spacing scale (docs/design.md §7). Use these instead of ad hoc dp values. */
object AstraSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Corner radius scale (docs/design.md §13). */
object AstraRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

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
