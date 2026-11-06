/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.home.impl.filters

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.filters.selection.FilterSelectionState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text

/** Single-choice filters keep the private home compact and avoid hidden combinations. */
@Composable
internal fun PrivateRoomFilterMenu(state: RoomListFiltersState, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFilter = state.selectedFilters().singleOrNull()
    val currentLabel = stringResource(selectedFilter?.stringResource ?: R.string.screen_private_filter_all)
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            BadgedBox(badge = { if (state.hasAnyFilterSelected) Badge() }) {
                Icon(CompoundIcons.Filter(), stringResource(R.string.screen_private_filter_current, currentLabel))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf<RoomListFilter?>(null) + RoomListFilter.entries).forEach { filter ->
                DropdownMenuItem(
                    modifier = Modifier.semantics { selected = filter == selectedFilter },
                    text = { Text(stringResource(filter?.stringResource ?: R.string.screen_private_filter_all)) },
                    trailingIcon = {
                        if (filter == selectedFilter) Icon(CompoundIcons.Check(), null)
                    },
                    onClick = {
                        state.eventSink(RoomListFiltersEvent.ClearSelectedFilters)
                        filter?.let { state.eventSink(RoomListFiltersEvent.ToggleFilter(it)) }
                        expanded = false
                    },
                )
            }
        }
    }
}

internal class PrivateRoomFilterPreviewParam : PreviewParameterProvider<RoomListFiltersState> {
    override val values = (listOf<RoomListFilter?>(null) + RoomListFilter.entries).asSequence().map { selected ->
        aRoomListFiltersState(RoomListFilter.entries.map { FilterSelectionState(it, isSelected = it == selected) })
    }
}

@PreviewsDayNight
@Composable
internal fun PrivateRoomFilterMenuPreview(@PreviewParameter(PrivateRoomFilterPreviewParam::class) state: RoomListFiltersState) = ElementPreview {
    PrivateRoomFilterMenu(state)
}
