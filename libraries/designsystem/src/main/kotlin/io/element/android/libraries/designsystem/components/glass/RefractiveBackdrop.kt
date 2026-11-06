/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.element.android.libraries.designsystem.motion.LocalReducedMotion
import io.element.android.libraries.designsystem.motion.rememberReducedMotion

val LocalRefractiveBackdrop = staticCompositionLocalOf<HazeState?> { null }

/** One live wallpaper capture, with independently refracting foreground surfaces. */
@Composable
fun RefractiveBackdrop(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberHazeState()
    val reducedMotion = rememberReducedMotion()
    Box(modifier) {
        // Never place content inside this source: messages would sample themselves.
        Box(Modifier.matchParentSize().hazeSource(state), content = background)
        CompositionLocalProvider(LocalRefractiveBackdrop provides state, LocalReducedMotion provides reducedMotion) {
            content()
        }
    }
}
