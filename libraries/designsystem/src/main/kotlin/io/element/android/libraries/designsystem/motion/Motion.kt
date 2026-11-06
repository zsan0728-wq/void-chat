/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.motion

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object MotionDuration {
    const val theme = 280
    const val fade = 180
}

object MotionOffset {
    /** Outgoing bubbles rise from the composer direction; incoming ones slide in from their side. */
    val outgoingSlide = 40.dp
    val incomingSlide = 14.dp
}

object MotionSpring {
    fun <T> message(): SpringSpec<T> = spring(dampingRatio = 0.82f, stiffness = 420f)
    fun <T> navigation(): SpringSpec<T> = spring(dampingRatio = 0.86f, stiffness = 380f)
    fun <T> interaction(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 580f)
}

object MotionEasing {
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

object MotionScale {
    const val messageInitial = 0.96f
    const val pressed = 1.025f
    const val pageInitial = 0.985f
}

object MotionTransition {
    fun enter(reducedMotion: Boolean = false): EnterTransition = if (reducedMotion) {
        EnterTransition.None
    } else {
        fadeIn(MotionSpring.navigation()) + scaleIn(MotionSpring.navigation(), initialScale = MotionScale.pageInitial)
    }

    fun exit(reducedMotion: Boolean = false): ExitTransition = if (reducedMotion) {
        ExitTransition.None
    } else {
        fadeOut(MotionSpring.navigation()) + scaleOut(MotionSpring.navigation(), targetScale = MotionScale.pageInitial)
    }
}

/** Shared at a backdrop boundary so a scrolling timeline needs only one system observer. */
val LocalReducedMotion = staticCompositionLocalOf<Boolean?> { null }

/** Reacts while the screen is open, including Accessibility's Remove animations setting. */
@Composable
fun rememberReducedMotion(): Boolean {
    LocalReducedMotion.current?.let { return it }
    val resolver = LocalContext.current.contentResolver
    fun animationsDisabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reducedMotion by remember(resolver) { mutableStateOf(animationsDisabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = animationsDisabled()
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reducedMotion
}
