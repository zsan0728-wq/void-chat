/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-2.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.screenshot.utils.createPaparazziRule
import io.element.android.libraries.designsystem.theme.components.Text
import org.junit.Rule
import org.junit.Test

class SmokeScreenshotTest {
    @get:Rule
    val paparazzi = createPaparazziRule()

    @Test
    fun smoke() {
        paparazzi.snapshot(name = "smoke") {
            ElementPreview {
                Text(
                    text = "Headless render OK",
                    style = ElementTheme.typography.fontHeadingLgBold,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
