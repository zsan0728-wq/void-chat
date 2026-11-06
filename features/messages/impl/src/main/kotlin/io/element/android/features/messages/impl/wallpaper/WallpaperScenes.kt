/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import io.element.android.compound.theme.ElementTheme

/**
 * Procedurally drawn, strictly grayscale wallpaper presets. They are ordinary Compose draw content,
 * so they behave exactly like a photographic backdrop as far as the Haze glass refraction is
 * concerned, while staying theme-aware, deterministic and free of bundled bitmap assets.
 */
@Composable
internal fun WallpaperScene(preset: WallpaperPreset, modifier: Modifier = Modifier) {
    val light = ElementTheme.isLightTheme
    Canvas(modifier.fillMaxSize()) {
        when (preset) {
            WallpaperPreset.Concrete -> drawConcrete(light)
            WallpaperPreset.Mountain -> drawMountain(light)
            WallpaperPreset.Fog -> drawFog(light)
        }
    }
}

private fun DrawScope.drawConcrete(light: Boolean) {
    val ground = if (light) Color(0xFFE5E3E0) else Color(0xFF171819)
    val concrete = if (light) Color(0xFF969A9A) else Color(0xFF505354)
    drawRect(ground)
    val facade = Path().apply {
        moveTo(size.width * .32f, size.height)
        lineTo(size.width * .32f, size.height * .4f)
        lineTo(size.width, size.height * .22f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(
        facade,
        Brush.linearGradient(listOf(concrete, ground), Offset.Zero, Offset(size.width, size.height)),
        style = Fill,
    )
    repeat(14) { index ->
        val x = size.width * (.32f + index * .052f)
        drawLine(concrete.copy(alpha = .45f), Offset(x, size.height * .42f), Offset(x, size.height), strokeWidth = 1.5f)
    }
    repeat(11) { index ->
        val y = size.height * (.43f + index * .056f)
        drawLine(ground.copy(alpha = .7f), Offset(size.width * .32f, y), Offset(size.width, y - size.height * .18f), strokeWidth = 2f)
    }
}

private fun DrawScope.drawMountain(light: Boolean) {
    val skyTop = if (light) Color(0xFFC9C9C9) else Color(0xFF0E0E0E)
    val skyBottom = if (light) Color(0xFFEDEDED) else Color(0xFF242424)
    drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

    // Soft atmospheric glow behind the dominant peak.
    val glow = if (light) Color.White.copy(alpha = .5f) else Color(0xFF3A3A3A).copy(alpha = .45f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(glow, Color.Transparent),
            center = Offset(size.width * .5f, size.height * .34f),
            radius = size.width * .6f,
        ),
        center = Offset(size.width * .5f, size.height * .34f),
        radius = size.width * .6f,
    )

    // Back range: lighter and higher, with a sharp central peak.
    val backTop = if (light) Color(0xFFB5B5B5) else Color(0xFF303030)
    val backBase = if (light) Color(0xFF8E8E8E) else Color(0xFF454545)
    val backBaseline = size.height * .68f
    val backPeaks = listOf(
        0f to .60f, .07f to .54f, .14f to .57f, .20f to .50f, .27f to .53f,
        .34f to .45f, .40f to .48f, .46f to .38f, .50f to .29f, .54f to .37f,
        .60f to .42f, .66f to .40f, .72f to .46f, .79f to .43f, .85f to .49f,
        .92f to .47f, 1f to .52f,
    )
    drawJaggedRange(backPeaks, backBaseline, Brush.verticalGradient(listOf(backTop, backBase)))

    // Near-white snow facet on the central peak.
    val snow = if (light) Color(0xFFF4F4F4) else Color(0xFF6A6A6A)
    val cap = Path().apply {
        moveTo(size.width * .50f, size.height * .29f)
        lineTo(size.width * .455f, size.height * .395f)
        lineTo(size.width * .50f, size.height * .365f)
        lineTo(size.width * .555f, size.height * .395f)
        close()
    }
    drawPath(cap, snow)

    // Mist band between the two ranges.
    drawMistBand(.60f, .74f, light)

    // Front range: darker and lower with its own jagged silhouette.
    val frontTop = if (light) Color(0xFF6E6E6E) else Color(0xFF262626)
    val frontBase = if (light) Color(0xFF4A4A4A) else Color(0xFF1A1A1A)
    val frontBaseline = size.height * .88f
    val frontPeaks = listOf(
        0f to .80f, .08f to .70f, .15f to .76f, .23f to .66f, .31f to .73f,
        .40f to .64f, .48f to .72f, .56f to .63f, .65f to .71f, .73f to .65f,
        .82f to .72f, .90f to .68f, 1f to .74f,
    )
    drawJaggedRange(frontPeaks, frontBaseline, Brush.verticalGradient(listOf(frontTop, frontBase)))

    // Fog at the base, fading into the composer area.
    drawMistBand(.82f, .99f, light)
}

/** Builds a sharp, angular ridgeline from explicit (x, y) fraction vertices and fills it. */
private fun DrawScope.drawJaggedRange(
    peaks: List<Pair<Float, Float>>,
    baseline: Float,
    brush: Brush,
) {
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, peaks.first().second * size.height)
        for (index in 1 until peaks.size) {
            val (previousX, previousY) = peaks[index - 1]
            val (x, y) = peaks[index]
            // One deterministic angular crag between key vertices keeps the ridge jagged.
            val midX = (previousX + x) * .5f
            val jag = ((previousY + y) * .5f - .012f * pseudoRandom(index, (x * 100).toInt())).coerceAtLeast(0f)
            lineTo(midX * size.width, jag * size.height)
            lineTo(x * size.width, y * size.height)
        }
        lineTo(size.width, baseline)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, brush)
}

/** Soft horizontal fog band with transparent edges for atmospheric depth. */
private fun DrawScope.drawMistBand(topFraction: Float, bottomFraction: Float, light: Boolean) {
    val top = size.height * topFraction
    val bottom = size.height * bottomFraction
    val mist = if (light) Color.White.copy(alpha = .22f) else Color(0xFF262626).copy(alpha = .4f)
    drawRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, mist, Color.Transparent)),
        topLeft = Offset(0f, top),
        size = Size(size.width, bottom - top),
    )
}

private fun DrawScope.drawFog(light: Boolean) {
    val base = if (light) Color(0xFFECECEC) else Color(0xFF161616)
    drawRect(base)
    val blob = if (light) Color(0xFFBDBDBD) else Color(0xFF2E2E2E)
    val lightBlob = if (light) Color.White else Color(0xFF3A3A3A)
    val centers = listOf(
        Offset(size.width * .25f, size.height * .3f),
        Offset(size.width * .7f, size.height * .55f),
        Offset(size.width * .4f, size.height * .8f),
    )
    centers.forEachIndexed { index, center ->
        drawCircle(
            brush = Brush.radialGradient(
                listOf(if (index == 1) lightBlob else blob, Color.Transparent),
                center = center,
                radius = size.width * .5f,
            ),
            center = center,
            radius = size.width * .5f,
        )
    }
    repeat(4) { index ->
        val y = size.height * (.2f + index * .18f)
        val bandHeight = size.height * .05f
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, lightBlob.copy(alpha = .25f), Color.Transparent),
            ),
            topLeft = Offset(0f, y),
            size = Size(size.width, bandHeight),
        )
    }
}

/** Deterministic 0..1 value so the ridgelines look natural but are stable across frames. */
private fun pseudoRandom(layer: Int, segment: Int): Float {
    val value = ((layer + 1) * 11 + (segment + 3) * 37) % 100
    return value / 100f
}
