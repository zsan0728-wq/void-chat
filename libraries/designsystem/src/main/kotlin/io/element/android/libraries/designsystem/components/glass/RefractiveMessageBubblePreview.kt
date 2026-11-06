/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text

class RefractiveBubblePreviewParameter : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

/** Software previews verify layout; the hardware-accelerated lab verifies optical rendering. */
@PreviewsDayNight
@Composable
internal fun RefractiveMessageBubblePreview(
    @PreviewParameter(RefractiveBubblePreviewParameter::class) isMine: Boolean,
) = ElementPreview {
    RefractiveBackdrop(
        modifier = Modifier.size(320.dp, 240.dp),
        background = {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.Gray)
                repeat(12) { index ->
                    val x = index * size.width / 12
                    drawLine(Color.White, Offset(x, 0f), Offset(x + 80f, size.height), 12f)
                }
            }
        },
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val interactions = remember { MutableInteractionSource() }
            RefractiveMessageBubble(
                state = checkNotNull(LocalRefractiveBackdrop.current),
                isMine = isMine,
                shape = RoundedCornerShape(20.dp),
                interactionSource = interactions,
                modifier = Modifier.combinedClickable(
                    interactionSource = interactions,
                    indication = null,
                    onClick = {},
                    onLongClick = {},
                ),
            ) {
                Text("The project file is ready.\nLong press to inspect the surface.", Modifier.padding(16.dp))
            }
        }
    }
}
