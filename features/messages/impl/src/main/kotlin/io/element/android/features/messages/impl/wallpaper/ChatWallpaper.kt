/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.wallpaper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.components.glass.LocalRefractiveBackdrop
import io.element.android.libraries.designsystem.components.glass.RefractiveBackdrop
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class WallpaperPreset(val storageKey: String) {
    Concrete("concrete"),
    Mountain("mountain"),
    Fog("fog"),
    ;

    companion object {
        fun fromKey(key: String?): WallpaperPreset =
            entries.firstOrNull { it.storageKey == key } ?: Concrete
    }
}

/** Only this wallpaper is captured by hazeSource; messages, editor and controls are sibling layers. */
@Composable
internal fun ChatWallpaper(
    preset: WallpaperPreset,
    galleryUri: Uri?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        if (galleryUri != null) {
            AsyncImage(
                model = galleryUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            WallpaperScene(preset, Modifier.fillMaxSize())
        }
    }
}

internal class ChatWallpaperState {
    var preset by mutableStateOf(WallpaperPreset.Concrete)
    var galleryUri by mutableStateOf<Uri?>(null)
    var choose: () -> Unit = {}
    var selectPreset: (WallpaperPreset) -> Unit = {}
    var reset: () -> Unit = {}
}

@Composable
internal fun rememberChatWallpaper(roomKey: String): ChatWallpaperState {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("chat_wallpapers", Context.MODE_PRIVATE) }
    val state = remember(roomKey) { ChatWallpaperState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomKey) {
        val stored = withContext(Dispatchers.IO) { preferences.getString(roomKey, null) }
        when {
            stored == null -> {
                state.preset = WallpaperPreset.Concrete
                state.galleryUri = null
            }
            stored.startsWith(PRESET_PREFIX) -> {
                state.preset = WallpaperPreset.fromKey(stored.removePrefix(PRESET_PREFIX))
                state.galleryUri = null
            }
            else -> {
                // Current "gallery:" prefix or a legacy raw URI.
                state.galleryUri = Uri.parse(stored.removePrefix(PRESET_GALLERY))
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                state.galleryUri = uri
                state.preset = WallpaperPreset.Concrete
                scope.launch(Dispatchers.IO) {
                    preferences.edit().putString(roomKey, PRESET_GALLERY + uri.toString()).apply()
                }
            } catch (_: SecurityException) {
                Toast.makeText(context, R.string.screen_room_wallpaper_access_failed, Toast.LENGTH_LONG).show()
            }
        }
    }
    state.choose = { picker.launch(arrayOf("image/*")) }
    state.selectPreset = { preset ->
        state.preset = preset
        state.galleryUri = null
        scope.launch(Dispatchers.IO) {
            preferences.edit().putString(roomKey, PRESET_PREFIX + preset.storageKey).apply()
        }
    }
    state.reset = {
        state.preset = WallpaperPreset.Concrete
        state.galleryUri = null
        scope.launch(Dispatchers.IO) { preferences.edit().remove(roomKey).apply() }
    }
    return state
}

private const val PRESET_PREFIX = "preset:"
private const val PRESET_GALLERY = "gallery:"

@Composable
internal fun ChatWallpaperMenu(onOpenScreen: () -> Unit) {
    IconButton(onClick = onOpenScreen) {
        Icon(CompoundIcons.Image(), contentDescription = stringResource(R.string.screen_room_wallpaper))
    }
}

/** A whole standalone timeline (for example pinned messages) owns one visible backdrop. */
@Composable
internal fun RoomTimelineBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    if (LocalRefractiveBackdrop.current != null) {
        Box(modifier, content = content)
    } else {
        RefractiveBackdrop(
            modifier = modifier,
            background = { WallpaperScene(WallpaperPreset.Concrete) },
            content = content,
        )
    }
}
