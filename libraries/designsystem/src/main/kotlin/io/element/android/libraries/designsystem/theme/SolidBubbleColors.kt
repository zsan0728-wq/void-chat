/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-2.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.element.android.compound.theme.ElementTheme

/**
 * Solid, opaque chat bubble colors. Bubbles are no longer glass: outgoing and incoming use distinct
 * flat colors that follow the system light/dark theme, and an optional user override takes precedence.
 */

// Default opaque backgrounds (kept strictly neutral grays to match the monochrome design language).
private val MineBubbleLight = Color(0xFF1F1F1F)
private val OtherBubbleLight = Color(0xFFECECEC)
private val MineBubbleDark = Color(0xFF161616)
private val OtherBubbleDark = Color(0xFF2B2B2B)

// Content on the user's own (dark) bubble is always light, regardless of the system theme.
private val OnMineBubble = Color(0xFFF5F5F5)

// Contrasting text color used when the user picks a custom bubble background.
private val OnLightBubble = Color(0xFF1A1A1A)

/** User-selected solid bubble colors. A null field means "follow the system theme default". */
data class SolidBubbleColorOverrides(
    val mine: Color? = null,
    val other: Color? = null,
)

/** CompositionLocal carrying user-selected bubble colors; null fields fall back to the theme default. */
val LocalSolidBubbleColorOverrides = staticCompositionLocalOf { SolidBubbleColorOverrides() }

/** Default opaque background for a bubble, following the current theme unless overridden by the user. */
@Composable
fun defaultSolidBubbleColor(isMine: Boolean): Color {
    val overrides = LocalSolidBubbleColorOverrides.current
    val overridden = if (isMine) overrides.mine else overrides.other
    if (overridden != null) return overridden
    val light = ElementTheme.isLightTheme
    return if (isMine) {
        if (light) MineBubbleLight else MineBubbleDark
    } else {
        if (light) OtherBubbleLight else OtherBubbleDark
    }
}

/**
 * Content color that should be forced inside a bubble.
 *
 * When the user picks a custom color the text contrast is computed from its luminance. Otherwise the
 * user's own bubble is dark, so its content is forced light (a specified color) while incoming bubbles
 * keep the normal semantic colors (which already adapt to the theme), hence [Color.Unspecified].
 */
@Composable
fun solidBubbleContentColor(isMine: Boolean): Color {
    val overrides = LocalSolidBubbleColorOverrides.current
    val overridden = if (isMine) overrides.mine else overrides.other
    if (overridden != null) return contentColorForBackground(overridden)
    return if (isMine) OnMineBubble else Color.Unspecified
}

/** Contrasting text color for an opaque background: dark on light, light on dark. */
private fun contentColorForBackground(background: Color): Color =
    if (background.luminance() > 0.5f) OnLightBubble else OnMineBubble
