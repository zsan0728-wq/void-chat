/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.components.RoomListContentView
import io.element.android.features.home.impl.components.RoomListMenuAction
import io.element.android.features.home.impl.filters.PrivateRoomFilterMenu
import io.element.android.features.home.impl.filters.RoomListFiltersEvent
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.RoomListContextMenu
import io.element.android.features.home.impl.roomlist.RoomListDeclineInviteMenu
import io.element.android.features.home.impl.roomlist.RoomListEvent
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.features.home.impl.search.GlobalSearchEvent
import io.element.android.features.home.impl.search.GlobalSearchView
import io.element.android.features.home.impl.search.RoomListSearchView
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.libraries.androidutils.throttler.FirstThrottler
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.motion.MotionDuration
import io.element.android.libraries.designsystem.motion.MotionSpring
import io.element.android.libraries.designsystem.motion.rememberReducedMotion
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.lazyColumnContentPadding
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun HomeView(
    homeState: HomeState,
    onRoomClick: (RoomId, EventId?) -> Unit,
    onSettingsClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onSwitchToAccount: (SessionId) -> Unit,
    onSetUpRecoveryClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onStartChatClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onRoomSettingsClick: (roomId: RoomId) -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    onReportRoomClick: (roomId: RoomId) -> Unit,
    onDeclineInviteAndBlockUser: (roomSummary: RoomListRoomSummary) -> Unit,
    acceptDeclineInviteView: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leaveRoomView: @Composable () -> Unit,
) {
    val state: RoomListState = homeState.roomListState
    val coroutineScope = rememberCoroutineScope()
    val firstThrottler = remember { FirstThrottler(300, coroutineScope) }
    LaunchedEffect(homeState.journalRoom) {
        (homeState.journalRoom as? AsyncData.Success<RoomId>)?.let { result ->
            homeState.eventSink(HomeEvent.JournalOpened)
            onRoomClick(result.data, null)
        }
    }
    Box(modifier) {
        if (state.contextMenu is RoomListState.ContextMenu.Shown) {
            RoomListContextMenu(
                contextMenu = state.contextMenu,
                canReportRoom = state.canReportRoom,
                eventSink = state.eventSink,
                onRoomSettingsClick = onRoomSettingsClick,
                onReportRoomClick = onReportRoomClick,
            )
        }
        if (state.declineInviteMenu is RoomListState.DeclineInviteMenu.Shown) {
            RoomListDeclineInviteMenu(
                menu = state.declineInviteMenu,
                canReportRoom = state.canReportRoom,
                eventSink = state.eventSink,
                onDeclineAndBlockClick = onDeclineInviteAndBlockUser,
            )
        }


        leaveRoomView()

        HomeScaffold(
            state = homeState,
            onSetUpRecoveryClick = onSetUpRecoveryClick,
            onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
            onRoomClick = { roomId -> if (firstThrottler.canHandle()) onRoomClick(roomId, null) },
            onOpenSettings = { if (firstThrottler.canHandle()) onSettingsClick() },
            onStartChatClick = { if (firstThrottler.canHandle()) onStartChatClick() },
            onCreateSpaceClick = { if (firstThrottler.canHandle()) onCreateSpaceClick() },
            onMenuActionClick = onMenuActionClick,
            onSwitchToAccount = onSwitchToAccount,
        )

        if (state.globalSearchState.isEnabled) {
            GlobalSearchView(
                state = state.globalSearchState,
                onSelectSearchResult = { roomId, eventId -> if (firstThrottler.canHandle()) onRoomClick(roomId, eventId) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElementTheme.colors.bgCanvasDefault),
            )
        } else {
            // This overlaid view will only be visible when state.displaySearchResults is true
            RoomListSearchView(
                state = state.searchState,
                eventSink = state.eventSink,
                hideInvitesAvatars = state.hideInvitesAvatars,
                onRoomClick = { roomId -> if (firstThrottler.canHandle()) onRoomClick(roomId, null) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElementTheme.colors.bgCanvasDefault)
            )
        }

        acceptDeclineInviteView()
    }
}

@Composable
private fun HomeScaffold(
    state: HomeState,
    onSetUpRecoveryClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onRoomClick: (RoomId) -> Unit,
    onOpenSettings: () -> Unit,
    onStartChatClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    onSwitchToAccount: (SessionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onRoomClick(room: RoomListRoomSummary) {
        onRoomClick(room.roomId)
    }

    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    val roomListState = state.roomListState
    val roomsLazyListState = rememberLazyListState()
    val backgroundState = rememberHazeState()
    val reducedMotion = rememberReducedMotion()
    // Start with all chats; the compact filter menu keeps filtering explicitly accessible.
    LaunchedEffect(Unit) {
        roomListState.filtersState.eventSink(RoomListFiltersEvent.ClearSelectedFilters)
        (roomListState.spaceFiltersState as? SpaceFiltersState.Selected)?.eventSink?.invoke(SpaceFiltersEvent.Selected.ClearSelection)
    }
    val backgroundColor by animateColorAsState(
        ElementTheme.colors.bgCanvasDefault,
        animationSpec = if (reducedMotion) snap() else tween(MotionDuration.theme),
        label = "home theme background",
    )
    BackHandler(enabled = state.isBackHandlerEnabled) {
        state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Chats))
        (roomListState.spaceFiltersState as? SpaceFiltersState.Selected)?.eventSink?.invoke(SpaceFiltersEvent.Selected.ClearSelection)
    }
    Scaffold(
        modifier = modifier.background(backgroundColor),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                ) {
                    AnimatedContent(
                        targetState = state.currentHomeNavigationBarItem,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            if (reducedMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                val forward = targetState.ordinal > initialState.ordinal
                                if (forward) {
                                    (slideInVertically(MotionSpring.navigation()) { it } + fadeIn(MotionSpring.navigation())) togetherWith
                                        (slideOutVertically(MotionSpring.navigation()) { -it } + fadeOut(MotionSpring.navigation()))
                                } else {
                                    (slideInVertically(MotionSpring.navigation()) { -it } + fadeIn(MotionSpring.navigation())) togetherWith
                                        (slideOutVertically(MotionSpring.navigation()) { it } + fadeOut(MotionSpring.navigation()))
                                }
                            }
                        },
                        label = "home top bar",
                    ) { item ->
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(item.labelRes),
                                modifier = Modifier.weight(1f),
                                style = ElementTheme.typography.fontHeadingLgBold.copy(fontSize = 34.sp, fontWeight = FontWeight.Black),
                            )
                            when (item) {
                                HomeNavigationBarItem.Chats -> {
                                    PrivateRoomFilterMenu(roomListState.filtersState)
                                    IconButton(onClick = {
                                        if (roomListState.globalSearchState.isEnabled) {
                                            roomListState.globalSearchState.eventSink(GlobalSearchEvent.ToggleSearchVisibility)
                                        } else {
                                            roomListState.eventSink(RoomListEvent.ToggleSearchResults)
                                        }
                                    }) { Icon(CompoundIcons.Search(), stringResource(CommonStrings.action_search)) }
                                    IconButton(onClick = onStartChatClick) { Icon(CompoundIcons.Plus(), stringResource(CommonStrings.action_start_chat)) }
                                }
                                HomeNavigationBarItem.Profile -> {
                                    IconButton(onClick = onOpenSettings) { Icon(CompoundIcons.Settings(), stringResource(CommonStrings.common_settings)) }
                                }
                                HomeNavigationBarItem.Contacts -> Unit
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            PrivateHomeNavigation(
                state = backgroundState,
                selected = state.currentHomeNavigationBarItem,
                unreadCount = state.totalUnreadCount,
                onSelect = { state.eventSink(HomeEvent.SelectHomeNavigationBarItem(it)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp),
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        contentWindowInsets = scaffoldScrollableContentInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // Only the page is a capture source: the glass navigation is a sibling outside it.
        AnimatedContent(
            targetState = state.currentHomeNavigationBarItem,
            modifier = Modifier.fillMaxSize().hazeSource(backgroundState).background(backgroundColor),
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val forward = targetState.ordinal > initialState.ordinal
                    val enter = if (forward) {
                        slideInHorizontally(MotionSpring.navigation()) { it / 6 }
                    } else {
                        slideInHorizontally(MotionSpring.navigation()) { -it / 6 }
                    }
                    val exit = if (forward) {
                        slideOutHorizontally(MotionSpring.navigation()) { -it / 8 }
                    } else {
                        slideOutHorizontally(MotionSpring.navigation()) { it / 8 }
                    }
                    (enter + fadeIn(MotionSpring.navigation())) togetherWith (exit + fadeOut(MotionSpring.navigation()))
                }
            },
            label = "home page",
        ) { page ->
            val pageModifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            when (page) {
                HomeNavigationBarItem.Chats -> RoomListContentView(
                    contentState = roomListState.contentState,
                    filtersState = roomListState.filtersState,
                    spaceFiltersState = roomListState.spaceFiltersState,
                    lazyListState = roomsLazyListState,
                    hideInvitesAvatars = roomListState.hideInvitesAvatars,
                    eventSink = roomListState.eventSink,
                    onSetUpRecoveryClick = onSetUpRecoveryClick,
                    onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
                    onRoomClick = ::onRoomClick,
                    onCreateRoomClick = onStartChatClick,
                    contentPadding = lazyColumnContentPadding + PaddingValues(bottom = 112.dp),
                    modifier = pageModifier,
                )
                HomeNavigationBarItem.Contacts -> PrivateContacts(state.contacts, onRoomClick, onStartChatClick, pageModifier)
                HomeNavigationBarItem.Profile -> state.currentUserAndNeighbors.getOrNull(if (state.currentUserAndNeighbors.size > 1) 1 else 0)?.let { user ->
                    PrivateProfile(
                        user = user,
                        onOpenSettings = onOpenSettings,
                        journalRoom = state.journalRoom,
                        onOpenJournal = { state.eventSink(HomeEvent.OpenJournal) },
                        onResetJournal = { state.eventSink(HomeEvent.ResetJournal) },
                        modifier = pageModifier,
                    )
                }
            }
        }
    }
}

internal fun RoomListRoomSummary.contentType() = displayType.ordinal

@PreviewsDayNight
@Composable
internal fun HomeViewPreview(@PreviewParameter(HomeStatePreviewParam::class) state: HomeState) = ElementPreview {
    HomeView(
        homeState = state,
        onRoomClick = { _, _ -> },
        onSettingsClick = {},
        onAddAccountClick = {},
        onSwitchToAccount = {},
        onSetUpRecoveryClick = {},
        onConfirmRecoveryKeyClick = {},
        onStartChatClick = {},
        onCreateSpaceClick = {},
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        onMenuActionClick = {},
        onDeclineInviteAndBlockUser = {},
        acceptDeclineInviteView = {},
        leaveRoomView = {}
    )
}

@Preview
@Composable
internal fun HomeViewA11yPreview() = ElementPreview {
    HomeView(
        homeState = aHomeState(),
        onRoomClick = { _, _ -> },
        onSettingsClick = {},
        onAddAccountClick = {},
        onSwitchToAccount = {},
        onSetUpRecoveryClick = {},
        onConfirmRecoveryKeyClick = {},
        onStartChatClick = {},
        onCreateSpaceClick = {},
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        onMenuActionClick = {},
        onDeclineInviteAndBlockUser = {},
        acceptDeclineInviteView = {},
        leaveRoomView = {}
    )
}
