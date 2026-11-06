/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.glass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.glass.GlassOptics
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.RefractionProfile

/**
 * Shared appearance for the floating glass islands (navigation bar, room top bar, composer and pinned
 * banner). The tint is intentionally light so the surface stays transparent, while the refractive optics
 * are stronger than the regular glass preset so content passing underneath is visibly bent.
 */

/** Theme-aware, mostly-transparent island tint. */
fun islandGlassTint(light: Boolean): Color =
    if (light) Color.White.copy(alpha = 0.28f) else Color(0xFF1B1B1B).copy(alpha = 0.28f)

/** Stronger-than-default refractive optics for the floating islands. */
fun islandGlassOptics(): GlassOptics = GlassOptics.regular.copy(
    refractionStrength = 1.0f,
    refractionDisplacement = 40.dp,
    refractionProfile = RefractionProfile.Edge(24.dp),
    blurRadius = OpticalSizeValue.Fixed(10.dp),
    depth = OpticalSizeValue.Fixed(0.6f),
    refractionDetailIntensity = 0.25f,
)

/** A rounded refractive island glass style with a transparent theme tint and reduced specular. */
fun refractiveIslandGlass(shape: RoundedCornerShape, light: Boolean): GlassStyle = GlassStyle.regular.then {
    shape(shape)
    tint(islandGlassTint(light))
    optics(islandGlassOptics())
    specularIntensity(0.14f)
    ambientResponse(0.12f)
    edgeSoftness(1.5.dp)
}
