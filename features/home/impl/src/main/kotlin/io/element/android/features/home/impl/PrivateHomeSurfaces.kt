/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.glass.hazeGlassContainer
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.glass.refractiveIslandGlass
import io.element.android.libraries.designsystem.motion.MotionSpring
import io.element.android.libraries.designsystem.motion.rememberReducedMotion
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun PrivateHomeNavigation(
    state: HazeState,
    selected: HomeNavigationBarItem,
    unreadCount: Long,
    onSelect: (HomeNavigationBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val position by animateFloatAsState(
        selected.ordinal.toFloat(),
        if (reducedMotion) snap() else MotionSpring.navigation(),
        label = "navigation glass position",
    )
    val input = remember(state) { HazeInput.Sources(state) }
    val light = ElementTheme.isLightTheme
    val glass = remember(light) {
        refractiveIslandGlass(shape = RoundedCornerShape(28.dp), light = light)
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .hazeGlass(input, glass)
            .padding(5.dp)
            .hazeGlassContainer(mergeDistance = 16.dp, memberTransitionSpec = MotionSpring.navigation())
            .selectableGroup(),
    ) {
        val slotWidth = maxWidth / HomeNavigationBarItem.entries.size
        Box(
            Modifier
                .graphicsLayer { translationX = slotWidth.toPx() * position }
                .width(slotWidth)
                .height(54.dp)
                .hazeGlass(input, glass),
        )
        Row(Modifier.fillMaxSize()) {
            HomeNavigationBarItem.entries.forEach { item ->
                val label = stringResource(item.labelRes)
                val description = if (item == HomeNavigationBarItem.Chats && unreadCount > 0) {
                    label + ", " + stringResource(R.string.screen_private_home_unread, unreadCount)
                } else {
                    label
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .selectable(selected == item, role = Role.Tab, onClick = { onSelect(item) })
                        .semantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // The icons draw directly so their glyphs stay sharp; only the container and
                    // the moving selection slider sample the backdrop.
                    Icon(item.icon(item == selected), null, modifier = Modifier.size(26.dp))
                    Text(label, style = ElementTheme.typography.fontBodySmRegular.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun PrivateContacts(
    contacts: ImmutableList<PrivateContact>,
    onRoomClick: (RoomId) -> Unit,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(contacts, query) {
        val term = query.trim()
        val sorted = contacts.sortedBy { it.name.lowercase() }
        if (term.isEmpty()) sorted else sorted.filter { it.name.contains(term, ignoreCase = true) }
    }
    // Flatten into header/contact rows while recording each section's list index for the index rail.
    val sections = remember(filtered) { groupContacts(filtered) }
    val rows = remember(sections) {
        val flat = mutableListOf<ContactRow>()
        val headerIndex = linkedMapOf<Char, Int>()
        sections.forEach { section ->
            headerIndex[section.letter] = flat.size
            flat.add(ContactRow.Header(section.letter))
            section.contacts.forEach { flat.add(ContactRow.Item(it)) }
        }
        flat to headerIndex
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollTo: (Char) -> Unit = { letter ->
        rows.second[letter]?.let { index -> scope.launch { listState.scrollToItem(index) } }
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 40.dp, bottom = 112.dp),
        ) {
            item(key = "contact-search", contentType = "search") {
                ContactSearchField(query = query, onQueryChange = { query = it }, modifier = Modifier.padding(vertical = 12.dp))
            }
            items(rows.first, key = { row ->
                when (row) {
                    is ContactRow.Header -> "header-" + row.letter
                    is ContactRow.Item -> row.contact.roomId.value
                }
            }, contentType = { row -> if (row is ContactRow.Header) "header" else "contact" }) { row ->
                when (row) {
                    is ContactRow.Header -> Text(
                        row.letter.toString(),
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                        style = ElementTheme.typography.fontBodyMdRegular.copy(fontWeight = FontWeight.Black),
                        color = ElementTheme.colors.textSecondary,
                    )
                    is ContactRow.Item -> ContactRowView(row.contact, onRoomClick)
                }
            }
            item(key = "contact-footer", contentType = "footer") {
                if (filtered.isEmpty()) {
                    Text(stringResource(R.string.screen_private_home_contacts_empty), modifier = Modifier.padding(vertical = 24.dp))
                }
                Button(text = stringResource(CommonStrings.action_start_chat), onClick = onStartChat, modifier = Modifier.padding(top = 12.dp))
            }
        }
        AlphabetIndexRail(
            activeLetters = rows.second.keys,
            onSelect = scrollTo,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
        )
    }
}

@Composable
private fun ContactRowView(room: PrivateContact, onRoomClick: (RoomId) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onRoomClick(room.roomId) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(room.avatarData, AvatarType.User)
        Text(
            room.name,
            style = ElementTheme.typography.fontBodyMdRegular.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContactSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(CompoundIcons.Search(), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Box {
                if (query.isEmpty()) {
                    Text(stringResource(CommonStrings.action_search), color = ElementTheme.colors.textSecondary)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = ElementTheme.typography.fontBodyMdRegular.copy(color = ElementTheme.colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AlphabetIndexRail(
    activeLetters: Set<Char>,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .width(20.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val letter = letterAt(offset.y, size.height)
                    if (letter in activeLetters) onSelect(letter)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    val letter = letterAt(change.position.y, size.height)
                    if (letter in activeLetters) onSelect(letter)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactAlphabet.forEach { letter ->
            Text(
                letter.toString(),
                style = ElementTheme.typography.fontBodyMdRegular.copy(
                    fontSize = 9.sp,
                    fontWeight = if (letter in activeLetters) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (letter in activeLetters) ElementTheme.colors.textPrimary else ElementTheme.colors.textSecondary,
                modifier = Modifier.padding(vertical = 0.5.dp),
            )
        }
    }
}

private fun letterAt(y: Float, height: Int): Char {
    val fraction = (y / height).coerceIn(0f, 1f)
    val index = (fraction * ContactAlphabet.lastIndex).roundToInt()
    return ContactAlphabet[index]
}

private data class ContactSection(val letter: Char, val contacts: List<PrivateContact>)

private sealed interface ContactRow {
    data class Header(val letter: Char) : ContactRow
    data class Item(val contact: PrivateContact) : ContactRow
}

private fun groupContacts(contacts: List<PrivateContact>): List<ContactSection> {
    val grouped = linkedMapOf<Char, MutableList<PrivateContact>>()
    contacts.forEach { contact ->
        val first = contact.name.trim().firstOrNull()?.uppercaseChar()
        val letter = if (first != null && first in 'A'..'Z') first else '#'
    grouped.getOrPut(letter) { mutableListOf() }.add(contact)
    }
    // Keep '#' last if present.
    return grouped.map { (letter, list) -> ContactSection(letter, list) }.sortedBy { if (it.letter == '#') Char.MAX_VALUE else it.letter }
}

private val ContactAlphabet: List<Char> = ('A'..'Z').toList() + '#'

@Composable
internal fun PrivateProfile(
    user: MatrixUser,
    onOpenSettings: () -> Unit,
    journalRoom: AsyncData<RoomId>,
    onOpenJournal: () -> Unit,
    onResetJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(user.userId) { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val avatar = user.getAvatarData(AvatarSize.UserHeader)
    BackHandler(expanded) { expanded = false }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .padding(bottom = 96.dp),
    ) {
        val sizeSpec = if (reducedMotion) snap() else MotionSpring.navigation<Dp>()
        // Single composition: the avatar width is an animated value, so every frame re-measures and
        // the lower elements are re-placed to their new position instead of cross-fading.
        val avatarWidth by animateDpAsState(
            targetValue = if (expanded) maxWidth else 144.dp,
            animationSpec = sizeSpec,
            label = "profile avatar width",
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Overlay, not a row: the name lives in a fixed-height layer and never moves or fades.
            // The avatar is drawn on top and grows leftward to occlude the name.
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .height(144.dp)
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        user.displayName ?: user.userId.value.substringAfter('@').substringBefore(':'),
                        style = ElementTheme.typography.fontHeadingLgBold.copy(
                            fontSize = 42.sp,
                            lineHeight = 44.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                        ),
                        // Keep clear of the collapsed avatar; once it grows it covers the text.
                        modifier = Modifier.padding(end = 144.dp + 20.dp),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .width(avatarWidth)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ElementTheme.colors.bgSubtleSecondary)
                        .clickable(
                            onClickLabel = stringResource(
                                if (expanded) R.string.screen_private_home_close_avatar else R.string.screen_private_home_open_avatar
                            ),
                        ) { expanded = !expanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(avatar.initialLetter, style = ElementTheme.typography.fontHeadingLgBold.copy(fontSize = 48.sp, fontWeight = FontWeight.Black))
                    AsyncImage(model = avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Text(user.userId.value, style = ElementTheme.typography.fontBodySmRegular)
            Button(stringResource(R.string.screen_private_home_edit_profile), onClick = onOpenSettings)
            Text(stringResource(R.string.screen_private_home_journal), style = ElementTheme.typography.fontHeadingLgBold)
            Text(stringResource(R.string.screen_private_home_journal_description))
            Button(
                stringResource(R.string.screen_private_home_open_journal),
                onClick = onOpenJournal,
                showProgress = journalRoom.isLoading(),
                enabled = !journalRoom.isLoading(),
            )
            TextButton(
                text = stringResource(R.string.screen_private_home_reset_journal),
                onClick = onResetJournal,
                enabled = !journalRoom.isLoading(),
            )
            if (journalRoom.isFailure()) {
                val message = if ((journalRoom as? AsyncData.Failure)?.error is JournalOpenError.NotReady) {
                    stringResource(R.string.screen_private_home_journal_error_syncing)
                } else {
                    stringResource(R.string.screen_private_home_journal_error)
                }
                Text(message, color = ElementTheme.colors.textCriticalPrimary)
            }
        }
    }
}
