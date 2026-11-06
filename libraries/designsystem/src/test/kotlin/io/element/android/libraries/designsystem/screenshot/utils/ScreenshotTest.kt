/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-2.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.screenshot.utils

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import com.android.resources.Density
import com.android.resources.NightMode

// Screenshots are captured at xxhdpi (480dpi / 160dpi = scale 3).
private const val DENSITY_SCALE = 3

// Upper bounds for self-sizing screenshots, in dp. RenderingMode.SHRINK crops each image to content.
private const val MAX_WIDTH_DP = 1080
private const val MAX_HEIGHT_DP = 2400

// Default phone frame used for full-screen components (islands, timeline, edge-to-edge checks).
const val PHONE_WIDTH_DP = 360
const val PHONE_HEIGHT_DP = 780

/**
 * Creates a [Paparazzi] rule for headless designsystem screenshots (no device needed).
 *
 * By default each screenshot sizes itself to its content ([RenderingMode.SHRINK]). Pass [widthDp]
 * and [heightDp] for previews that rely on fillMaxSize/weight (then a fixed frame is used).
 *
 * Note: real Haze blur/refraction is not supported by LayoutLib and will render as a fallback, so
 * these screenshots verify layout, shape, color and spacing only; the glass look is checked on device.
 */
fun createPaparazziRule(
    widthDp: Int? = null,
    heightDp: Int? = null,
    darkMode: Boolean = false,
): Paparazzi {
    val selfSizing = widthDp == null || heightDp == null
    return Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = (widthDp ?: MAX_WIDTH_DP) * DENSITY_SCALE,
            screenHeight = (heightDp ?: MAX_HEIGHT_DP) * DENSITY_SCALE,
            xdpi = 480,
            ydpi = 480,
            density = Density.XXHIGH,
            softButtons = false,
        ).copy(nightMode = if (darkMode) NightMode.NIGHT else NightMode.NOTNIGHT),
        renderingMode = if (selfSizing) RenderingMode.SHRINK else RenderingMode.NORMAL,
        supportsRtl = true,
        useDeviceResolution = true,
        maxPercentDifference = 0.0,
    )
}
