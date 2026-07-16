package com.astramesh.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AstraMesh color tokens — black-first monochrome (see docs/design.md §5).
 * The product must still read clearly if the single accent is removed.
 */

// Base palette
val AstraBlack = Color(0xFF000000)       // true black background
val AstraSurface = Color(0xFF0B0B0B)     // elevated surface
val AstraPanel = Color(0xFF121212)       // cards and panels
val AstraBorder = Color(0xFF1A1A1A)      // borders / separators
val AstraTextPrimary = Color(0xFFEDEDED) // primary text
val AstraTextSecondary = Color(0xFFA8A8A8)
val AstraTextDisabled = Color(0xFF6B6B6B)

// Restrained accent + status colors (used sparingly)
val AstraAccent = Color(0xFF4FB0C6)      // muted steel/cyan — active
val AstraSuccess = Color(0xFF5FA777)     // delivered / success
val AstraWarning = Color(0xFFC79A4B)     // pending / warning
val AstraCritical = Color(0xFFC65B5B)    // emergency / failure
