/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.actionlist

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/** Layout-only coordinates: scrolling must not recompose the message tree. */
internal class MessageActionAnchors {
    private val bounds = mutableMapOf<String, Rect>()
    fun update(id: String, rect: Rect) { bounds[id] = rect }
    fun remove(id: String) { bounds.remove(id) }
    fun get(id: String): Rect? = bounds[id]
}

internal val LocalMessageActionAnchors = staticCompositionLocalOf<MessageActionAnchors?> { null }
