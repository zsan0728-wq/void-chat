/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.glass

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/**
 * On-glass content color for the surrounding refractive bubble.
 *
 * A specified value means the bubble is a dark surface (the user's own outgoing bubble) and every
 * content component inside it should render light for readability. When [Color.Unspecified], the
 * bubble is a light surface and components keep their normal semantic colors.
 */
val LocalGlassBubbleContentColor = compositionLocalOf { Color.Unspecified }

/** Primary content color on the dark glass, or the supplied fallback when the bubble is light. */
fun Color?.onGlassOr(fallback: Color): Color =
    if (this != null && isSpecified) this else fallback

/** Secondary content color: the on-glass color at reduced opacity, or the supplied fallback. */
fun Color?.onGlassSecondaryOr(fallback: Color, alpha: Float = 0.62f): Color =
    if (this != null && isSpecified) copy(alpha = alpha) else fallback
