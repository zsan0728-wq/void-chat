/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.wallpaper

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.theme.components.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-selected opaque bubble colors. A null value means "follow the system theme default"; the text
 * contrast is computed automatically from the chosen background in [io.element.android.libraries.designsystem.theme.solidBubbleContentColor].
 */
internal class BubbleColorsState {
    var mine by mutableStateOf<Color?>(null)
    var other by mutableStateOf<Color?>(null)
    var selectMine: (Color?) -> Unit = {}
    var selectOther: (Color?) -> Unit = {}
    var reset: () -> Unit = {}
}

/** Pickable solid bubble colors. Both light and dark entries are offered so text contrast adapts. */
internal val BubbleColorPalette: List<Color> = listOf(
    Color(0xFF161616),
    Color(0xFF3A3A3A),
    Color(0xFF6E6E6E),
    Color(0xFFB9B9B9),
    Color(0xFFF0F0F0),
    Color(0xFF4C5A9E),
    Color(0xFF3E7A5E),
    Color(0xFFA85B4A),
    Color(0xFF7A5A9E),
)

@Composable
internal fun rememberBubbleColors(roomKey: String): BubbleColorsState {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("chat_bubble_colors", Context.MODE_PRIVATE) }
    val state = remember(roomKey) { BubbleColorsState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomKey) {
        val stored = withContext(Dispatchers.IO) { preferences.getString(roomKey, null) }
        val (mine, other) = deserialize(stored)
        state.mine = mine
        state.other = other
    }

    fun persist() {
        scope.launch(Dispatchers.IO) {
            preferences.edit().putString(roomKey, serialize(state.mine, state.other)).apply()
        }
    }

    state.selectMine = { color ->
        state.mine = color
        persist()
    }
    state.selectOther = { color ->
        state.other = color
        persist()
    }
    state.reset = {
        state.mine = null
        state.other = null
        scope.launch(Dispatchers.IO) { preferences.edit().remove(roomKey).apply() }
    }
    return state
}

private fun serialize(mine: Color?, other: Color?): String =
    "${mine?.toHexRgb() ?: ""}|${other?.toHexRgb() ?: ""}"

private fun deserialize(value: String?): Pair<Color?, Color?> {
    if (value.isNullOrBlank()) return null to null
    val parts = value.split("|", limit = 2)
    return parts.getOrNull(0).orEmpty().toColorOrNull() to parts.getOrNull(1).orEmpty().toColorOrNull()
}

private fun Color.toHexRgb(): String {
    val r = (red * 255 + 0.5f).toInt().coerceIn(0, 255)
    val g = (green * 255 + 0.5f).toInt().coerceIn(0, 255)
    val b = (blue * 255 + 0.5f).toInt().coerceIn(0, 255)
    return "%02X%02X%02X".format(r, g, b)
}

private fun String.toColorOrNull(): Color? = runCatching {
    val argb = android.graphics.Color.parseColor("#$this")
    Color(
        red = android.graphics.Color.red(argb) / 255f,
        green = android.graphics.Color.green(argb) / 255f,
        blue = android.graphics.Color.blue(argb) / 255f,
    )
}.getOrNull()

/**
 * Renders the incoming/outgoing color pickers used by [io.element.android.features.messages.impl.wallpaper.ChatBackgroundScreen].
 */
@Composable
internal fun BubbleColorSection(
    bubbleColors: BubbleColorsState,
) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.screen_room_bubble_colors_title),
        style = ElementTheme.typography.fontBodyMdRegular,
        color = ElementTheme.colors.textSecondary,
    )
    Spacer(Modifier.height(10.dp))
    BubbleColorRow(
        label = stringResource(R.string.screen_room_bubble_colors_outgoing),
        selected = bubbleColors.mine,
        onSelect = bubbleColors.selectMine,
    )
    Spacer(Modifier.height(14.dp))
    BubbleColorRow(
        label = stringResource(R.string.screen_room_bubble_colors_incoming),
        selected = bubbleColors.other,
        onSelect = bubbleColors.selectOther,
    )
}

@Composable
private fun BubbleColorRow(
    label: String,
    selected: Color?,
    onSelect: (Color?) -> Unit,
) {
    Text(text = label, style = ElementTheme.typography.fontBodySmMedium, color = ElementTheme.colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DefaultBubbleSwatch(selected = selected == null, onClick = { onSelect(null) })
        BubbleColorPalette.forEach { color ->
            BubbleColorSwatch(color = color, selected = selected == color, onClick = { onSelect(color) })
        }
    }
}

@Composable
private fun DefaultBubbleSwatch(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val ring = if (selected) ElementTheme.colors.textPrimary else ElementTheme.colors.borderDisabled
    Row(
        modifier = Modifier
            .clip(shape)
            .background(ElementTheme.colors.bgSubtleSecondary)
            .border(1.dp, ring, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.screen_room_bubble_colors_default),
            style = ElementTheme.typography.fontBodySmMedium,
            color = if (selected) ElementTheme.colors.textPrimary else ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun BubbleColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ring = if (selected) ElementTheme.colors.textPrimary else ElementTheme.colors.borderDisabled
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = if (selected) 2.5.dp else 1.dp, color = ring, shape = CircleShape)
            .clickable(onClick = onClick),
    )
}
