/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.wallpaper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.components.glass.LocalRefractiveBackdrop
import io.element.android.libraries.designsystem.components.glass.RefractiveBackdrop
import io.element.android.libraries.designsystem.components.glass.RefractiveMessageBubble
import io.element.android.libraries.designsystem.motion.MotionScale
import io.element.android.libraries.designsystem.motion.MotionSpring
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text

/**
 * Dedicated chat-background picker. The large preview is a real [RefractiveBackdrop] with a live
 * glass bubble, so presets and gallery images are shown through the same refraction used in the
 * room. All choices persist immediately through [wallpaper].
 */
@Composable
internal fun ChatBackgroundScreen(
    wallpaper: ChatWallpaperState,
    bubbleColors: BubbleColorsState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = MotionSpring.navigation(),
        label = "Chat background entrance",
    )

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = entrance
                val scale = MotionScale.pageInitial + (1f - MotionScale.pageInitial) * entrance
                scaleX = scale
                scaleY = scale
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(CompoundIcons.ChevronLeft(), contentDescription = null)
                }
                Text(
                    text = stringResource(R.string.screen_room_wallpaper_title),
                    style = ElementTheme.typography.fontHeadingSmMedium,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Live preview through a real glass bubble.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                RefractiveBackdrop(
                    background = {
                        ChatWallpaper(
                            preset = wallpaper.preset,
                            galleryUri = wallpaper.galleryUri,
                        )
                    },
                ) {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                        RefractiveMessageBubble(
                            state = requireNotNull(LocalRefractiveBackdrop.current),
                            isMine = true,
                            shape = RoundedCornerShape(22.dp),
                        ) {
                            Text(
                                text = "GLASS PREVIEW",
                                style = ElementTheme.typography.fontBodyMdRegular,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Custom",
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElementTheme.colors.bgSubtleSecondary),
            ) {
                CustomActionRow(
                    icon = CompoundIcons.Image(),
                    label = stringResource(R.string.screen_room_wallpaper_choose),
                    onClick = wallpaper.choose,
                )
                Box(Modifier.padding(start = 52.dp).height(1.dp).fillMaxWidth().background(ElementTheme.colors.borderDisabled))
                CustomActionRow(
                    icon = CompoundIcons.Grid(),
                    label = stringResource(R.string.screen_room_wallpaper_reset),
                    onClick = wallpaper.reset,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // Match the target order: mountain, fog, concrete.
                listOf(WallpaperPreset.Mountain, WallpaperPreset.Fog, WallpaperPreset.Concrete).forEach { preset ->
                    PresetThumbnail(
                        preset = preset,
                        selected = wallpaper.galleryUri == null && wallpaper.preset == preset,
                        onClick = { wallpaper.selectPreset(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            BubbleColorSection(bubbleColors)
        }
    }
}

@Composable
private fun CustomActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, modifier = Modifier.weight(1f), style = ElementTheme.typography.fontBodyMdRegular)
        Icon(CompoundIcons.ChevronRight(), contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PresetThumbnail(
    preset: WallpaperPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val ring = if (selected) ElementTheme.colors.textPrimary else ElementTheme.colors.borderDisabled
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .border(width = if (selected) 2.5.dp else 1.dp, color = ring, shape = shape)
            .clickable(onClick = onClick)
    ) {
        WallpaperScene(preset, Modifier.fillMaxSize())
        if (selected) {
            Icon(
                imageVector = CompoundIcons.Check(),
                contentDescription = null,
                tint = ElementTheme.colors.textPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp),
            )
        }
    }
}
