/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.motion

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.designsystem.motion.MotionOffset
import io.element.android.libraries.designsystem.motion.MotionScale
import io.element.android.libraries.designsystem.motion.MotionSpring
import io.element.android.libraries.designsystem.motion.rememberReducedMotion
import io.element.android.libraries.matrix.api.timeline.item.event.TimelineItemEventOrigin

internal data class MessageEntry(val id: String, val isMine: Boolean)

internal val LocalMessageEntry = staticCompositionLocalOf<MessageEntry?> { null }

internal fun TimelineItem.Event.entryCandidate() = MessageEntryCandidate(
    id = id.value,
    remoteId = eventId?.value,
    transactionId = transactionId?.value,
    origin = when {
        origin == TimelineItemEventOrigin.LOCAL || (eventId == null && transactionId != null) -> MessageEntryCandidate.Origin.Local
        origin == TimelineItemEventOrigin.SYNC -> MessageEntryCandidate.Origin.Sync
        else -> MessageEntryCandidate.Origin.History
    },
)

@Composable
internal fun rememberMessageEntry(event: TimelineItem.Event?, frame: MessageEntryFrame, reducedMotion: Boolean): MessageEntry? {
    if (event == null) return null
    val admitted = remember(event.id) { frame.isEligible(event.id.value, SystemClock.uptimeMillis()) }
    SideEffect { frame.claim(event.id.value, SystemClock.uptimeMillis()) }
    return remember(event.id, admitted, reducedMotion) {
        MessageEntry(event.id.value, event.isMine).takeIf { admitted && !reducedMotion }
    }
}

/**
 * Self-contained spring entrance. All transforms live on a GPU graphics layer, so the row's layout
 * position and Haze sampling stay anchored to the final bounds and never feed back into the motion.
 * An outgoing bubble rises from the composer direction; an incoming one slides from its own side.
 */
@Composable
internal fun MessageEntryContainer(content: @Composable BoxScope.() -> Unit) {
    val entry = LocalMessageEntry.current
    if (entry == null) {
        Box(content = content)
        return
    }
    val reducedMotion = rememberReducedMotion()
    val progress = remember(entry.id) { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(entry.id, reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, MotionSpring.message())
        }
    }
    if (progress.value >= 1f) {
        Box(content = content)
        return
    }
    val remaining = 1f - progress.value
    Box(
        Modifier.graphicsLayer {
            val scale = MotionScale.messageInitial + (1f - MotionScale.messageInitial) * progress.value
            scaleX = scale
            scaleY = scale
            alpha = progress.value.coerceIn(0f, 1f)
            transformOrigin = TransformOrigin.Center
            if (entry.isMine) {
                translationY = MotionOffset.outgoingSlide.toPx() * remaining
            } else {
                translationX = -MotionOffset.incomingSlide.toPx() * remaining
            }
        },
        content = content,
    )
}
