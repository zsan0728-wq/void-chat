/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeSourceRetention
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassOptics
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.motion.MotionDuration
import io.element.android.libraries.designsystem.motion.MotionScale
import io.element.android.libraries.designsystem.motion.MotionSpring
import io.element.android.libraries.designsystem.motion.rememberReducedMotion

/**
 * An independent optical boundary over the room's live background. The material samples the shared
 * [state] backdrop in real time and applies edge-concentrated refraction, depth blur, a Fresnel rim
 * and a faint specular highlight. Refraction is authored on an edge band so the centre where the
 * text sits stays undistorted and readable. Content is drawn afterwards, so message glyphs and
 * media are never inputs to their own refraction shader.
 *
 * Gesture ownership remains with the caller; share its interactionSource for visual feedback.
 */
@Composable
fun RefractiveMessageBubble(
    state: HazeState,
    isMine: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val pressure = animateFloatAsState(
        targetValue = if (pressed.value) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else MotionSpring.interaction(),
        label = "Glass pressure",
    )
    val light = ElementTheme.isLightTheme
    // Outgoing bubbles are dark glass with light content; incoming bubbles stay light.
    val defaultTint = if (light) {
        if (isMine) Color(0xFF2E2E2E) else Color.White
    } else {
        if (isMine) Color(0xFF0A0A0A) else Color(0xFF171717)
    }
    val tintAlpha = if (light) {
        if (isMine) 0.62f else 0.42f
    } else {
        if (isMine) 0.66f else 0.5f
    }
    val onGlassColor = if (isMine) Color(0xFFF2F2F2) else Color.Unspecified
    val surfaceTint = animateColorAsState(
        targetValue = tint ?: defaultTint.copy(alpha = tintAlpha),
        animationSpec = if (reducedMotion) snap() else tween(MotionDuration.theme),
        label = "Glass tint",
    )
    val currentShape = rememberUpdatedState(shape)
    val style = remember {
        GlassStyle.regular.then {
            // Haze owns the interaction-driven optical response and animates it with the press spring.
            pressed {
                animate(toSpec = MotionSpring.interaction(), fromSpec = MotionSpring.interaction()) {
                    refractionMultiplier(1.25f)
                    lightingIntensity(0.55f)
                    whitePointDelta(0.04f)
                }
            }
            shape(currentShape.value)
            optics(
                GlassOptics.regular.copy(
                    refractionStrength = 0.78f,
                    refractionDisplacement = 32.dp,
                    refractionProfile = RefractionProfile.Edge(20.dp),
                    blurRadius = OpticalSizeValue.Fixed(11.dp),
                    depth = OpticalSizeValue.Fixed(0.6f),
                    refractionDetailIntensity = 0f,
                )
            )
            tint(surfaceTint.value)
            specularIntensity(0.46f)
            ambientResponse(0.18f)
            edgeSoftness(1.5.dp)
            chromaticAberrationStrength(0.018f)
            lightPosition(Alignment.TopStart)
        }
    }
    val input = remember(state) {
        HazeInput.Sources(state, retention = HazeSourceRetention.ClearWhenUnavailable)
    }
    Box(
        modifier
            .graphicsLayer {
                val p = if (reducedMotion) 0f else pressure.value
                val scale = 1f + p * (MotionScale.pressed - 1f)
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .hazeGlass(
                input = input,
                style = style,
                performanceMode = HazePerformanceMode.Balanced,
                expandLayerBounds = true,
                interactionSource = source,
            ),
    ) {
        CompositionLocalProvider(LocalGlassBubbleContentColor provides onGlassColor) {
            content()
        }
    }
}
