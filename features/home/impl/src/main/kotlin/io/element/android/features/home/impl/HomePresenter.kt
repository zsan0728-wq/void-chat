/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.announcement.api.Announcement
import io.element.android.features.announcement.api.AnnouncementService
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.features.home.impl.spaces.HomeSpacesState
import io.element.android.features.rageshake.api.RageshakeFeatureAvailability
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.indicator.api.IndicatorService
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.sync.SyncService
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
class HomePresenter(
    private val client: MatrixClient,
    private val syncService: SyncService,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val indicatorService: IndicatorService,
    private val roomListPresenter: Presenter<RoomListState>,
    private val homeSpacesPresenter: Presenter<HomeSpacesState>,
    private val rageshakeFeatureAvailability: RageshakeFeatureAvailability,
    private val sessionStore: SessionStore,
    private val announcementService: AnnouncementService,
    private val featureFlagService: FeatureFlagService,
    private val privateJournal: PrivateJournal,
    @AppCoroutineScope
    private val appCoroutineScope: CoroutineScope,
) : Presenter<HomeState> {
    private val currentUserWithNeighborsBuilder = CurrentUserWithNeighborsBuilder()

    @Composable
    override fun present(): HomeState {
        val coroutineScope = rememberCoroutineScope()
        var journalRoom by remember { mutableStateOf<AsyncData<RoomId>>(AsyncData.Uninitialized) }
        val matrixUser by client.userProfile.collectAsState()
        // Use all locally synced rooms, independently of message-list paging and selected filters.
        val contacts by remember(client) {
            client.roomListService.allRooms.summaries.map { rooms ->
                rooms.filter { it.info.isDm && it.info.currentUserMembership == CurrentUserMembership.JOINED && it.info.successorRoom == null }
                    .distinctBy { it.info.heroes.singleOrNull()?.userId?.value ?: it.roomId.value }
                    .map { room -> PrivateContact(room.roomId, room.info.name ?: room.roomId.value, room.info.getAvatarData(AvatarSize.UserListItem)) }
                    .sortedBy { it.name.lowercase() }
                    .toImmutableList()
            }
        }.collectAsState(initial = persistentListOf())
        val totalUnreadCount by client.totalUnreadCountFlow.collectAsState()
        val currentUserAndNeighbors by remember {
            combine(
                client.userProfile,
                sessionStore.sessionsFlow(),
                currentUserWithNeighborsBuilder::build,
            )
        }.collectAsState(initial = persistentListOf(matrixUser))
        val isOnline by syncService.isOnline.collectAsState()
        val canReportBug by remember { rageshakeFeatureAvailability.isAvailable() }.collectAsState(false)
        val showMultiAccountAnnouncement by remember {
            combine(
                announcementService.announcementsToShowFlow(),
                featureFlagService.isFeatureEnabledFlow(FeatureFlags.MultiAccount),
                sessionStore.sessionsFlow(),
            ) { announcements, isMultiAccountEnabled, sessions ->
                // Do not announce the feature to users who already have several accounts
                isMultiAccountEnabled && sessions.size == 1 && announcements.contains(Announcement.MultiAccount)
            }
        }.collectAsState(false)
        LaunchedEffect(Unit) {
            // The user already uses several accounts, so the feature does not need to be announced anymore,
            // even if they later remove accounts.
            combine(
                announcementService.announcementsToShowFlow(),
                sessionStore.sessionsFlow(),
            ) { announcements, sessions ->
                announcements.contains(Announcement.MultiAccount) && sessions.size > 1
            }
                .filter { it }
                .collect {
                    announcementService.onAnnouncementDismissed(Announcement.MultiAccount)
                }
        }
        val roomListState = roomListPresenter.present()
        val homeSpacesState = homeSpacesPresenter.present()
        var currentHomeNavigationBarItemOrdinal by rememberSaveable { mutableIntStateOf(HomeNavigationBarItem.Chats.ordinal) }
        val currentHomeNavigationBarItem by remember {
            derivedStateOf {
                HomeNavigationBarItem.from(currentHomeNavigationBarItemOrdinal)
            }
        }
        LaunchedEffect(Unit) {
            // Force a refresh of the profile
            client.getUserProfile()
        }
        // Avatar indicator
        val showAvatarIndicator by indicatorService.showRoomListTopBarIndicator()

        fun openJournal() {
            if (!journalRoom.isLoading()) {
                journalRoom = AsyncData.Loading()
                coroutineScope.launch {
                    journalRoom = privateJournal.open().fold(
                        onSuccess = { AsyncData.Success(it) },
                        onFailure = { AsyncData.Failure(it) },
                    )
                }
            }
        }

        fun resetJournal() {
            coroutineScope.launch {
                privateJournal.reset()
                journalRoom = AsyncData.Uninitialized
            }
        }

        fun handleEvent(event: HomeEvent) {
            when (event) {
                HomeEvent.OpenJournal -> openJournal()
                HomeEvent.ResetJournal -> resetJournal()
                HomeEvent.JournalOpened -> journalRoom = AsyncData.Uninitialized
                is HomeEvent.SelectHomeNavigationBarItem -> {
                    currentHomeNavigationBarItemOrdinal = event.item.ordinal
                }
                // The user is switching account, which destroys this presenter, so use the app scope to not be cancelled.
                HomeEvent.DismissSwipeAvatarAnnouncement -> appCoroutineScope.launch {
                    announcementService.onAnnouncementDismissed(Announcement.SwipeAvatarToSwitchAccount)
                }
                HomeEvent.DismissMultiAccountAnnouncement -> coroutineScope.launch {
                    announcementService.onAnnouncementDismissed(Announcement.MultiAccount)
                }
            }
        }

        // Retry a transient "still syncing/offline" journal failure automatically once the network comes back.
        LaunchedEffect(isOnline) {
            if (isOnline && (journalRoom as? AsyncData.Failure)?.error is JournalOpenError.NotReady) {
                openJournal()
            }
        }

        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        return HomeState(
            currentUserAndNeighbors = currentUserAndNeighbors,
            showAvatarIndicator = showAvatarIndicator,
            hasNetworkConnection = isOnline,
            currentHomeNavigationBarItem = currentHomeNavigationBarItem,
            roomListState = roomListState,
            homeSpacesState = homeSpacesState,
            snackbarMessage = snackbarMessage,
            canReportBug = canReportBug,
            showMultiAccountAnnouncement = showMultiAccountAnnouncement,
            totalUnreadCount = totalUnreadCount,
            eventSink = ::handleEvent,
            contacts = contacts,
            journalRoom = journalRoom,
        )
    }
}
