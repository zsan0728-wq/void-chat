/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-2.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.glass.LocalGlassBubbleContentColor
import io.element.android.libraries.designsystem.screenshot.utils.createPaparazziRule
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.defaultSolidBubbleColor
import io.element.android.libraries.designsystem.theme.solidBubbleContentColor
import org.junit.Rule
import org.junit.Test

class SolidBubbleLightScreenshotTest {
    @get:Rule
    val paparazzi: Paparazzi = createPaparazziRule()

    @Test
    fun light() {
        paparazzi.snapshot { ElementTheme { BubbleSamples() } }
    }
}

class SolidBubbleDarkScreenshotTest {
    @get:Rule
    val paparazzi: Paparazzi = createPaparazziRule(darkMode = true)

    @Test
    fun dark() {
        paparazzi.snapshot { ElementTheme { BubbleSamples() } }
    }
}

@Composable
private fun BubbleSamples() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SampleBubble(isMine = false, text = "Incoming solid bubble. Normal semantic content color.")
        SampleBubble(isMine = true, text = "Outgoing solid bubble. Forced light content.")
    }
}

@Composable
private fun SampleBubble(isMine: Boolean, text: String) {
    val color = defaultSolidBubbleColor(isMine = isMine)
    val contentColor = solidBubbleContentColor(isMine = isMine)
    CompositionLocalProvider(LocalGlassBubbleContentColor provides contentColor) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .drawBehind { drawRect(color) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            // Mirror the adapted timeline components: forced light content on the dark outgoing bubble.
            Text(text = text, color = if (contentColor.isSpecified) contentColor else Color.Unspecified)
        }
    }
}
