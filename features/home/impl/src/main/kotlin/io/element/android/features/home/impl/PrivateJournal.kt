/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber

/** A journal is an actual encrypted Matrix room. Creating/opening it never sends a message. */

/** Reasons a journal cannot be opened, split so the UI can tell a transient sync problem from a real state problem. */
sealed class JournalOpenError(message: String) : Exception(message) {
    /** Transient: still syncing or offline. Retrying later may succeed. */
    class NotReady(message: String) : JournalOpenError(message)

    /** The room exists but is not a joined, encrypted room (or the bookmark is malformed). */
    class InvalidState(message: String) : JournalOpenError(message)
}

@Inject
class PrivateJournal(
    private val client: MatrixClient,
    private val dispatchers: CoroutineDispatchers,
) {
    private val mutex = Mutex()
    private var pendingRoom: RoomId? = null
    private val bookmark get() = File(client.sessionPaths.fileDirectory, "private-journal-room")

    suspend fun open(): Result<RoomId> = runCatchingExceptions {
        mutex.withLock {
            // A missing account-data event before initial sync must not create a second journal.
            if (!client.roomListService.isInitialSyncDone) {
                throw JournalOpenError.NotReady("Wait for initial sync before opening the journal")
            }
            val remote = client.getAccountData(ACCOUNT_DATA_TYPE).getOrThrow()
            val storedRoomId = parseAccountDataRoomId(remote)
            if (storedRoomId != null) {
                validateExistingRoom(storedRoomId)
                return@withLock storedRoomId
            }
            val local = pendingRoom ?: withContext(dispatchers.io) {
                bookmark.takeIf(File::exists)?.readText()?.let(::parseRoomId)
            }
            val roomId = if (local != null) {
                validateExistingRoom(local)
                local
            } else {
                client.createRoom(
                    CreateRoomParameters(
                        name = "MY JOURNAL",
                        isEncrypted = true,
                        visibility = RoomVisibility.Private,
                        preset = RoomPreset.PRIVATE_CHAT,
                        historyVisibilityOverride = RoomHistoryVisibility.Invited,
                        invite = emptyList(),
                    )
                ).getOrThrow()
            }
            pendingRoom = roomId
            // Persist before uploading account data, so a failed upload/restart retries the same room.
            withContext(dispatchers.io) {
                bookmark.parentFile?.mkdirs()
                val temporary = File(bookmark.parentFile, "${bookmark.name}.tmp")
                temporary.outputStream().use { stream ->
                    stream.write(roomId.value.toByteArray(Charsets.UTF_8))
                    stream.fd.sync()
                }
                try {
                    Files.move(temporary.toPath(), bookmark.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary.toPath(), bookmark.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
            client.setAccountData(
                ACCOUNT_DATA_TYPE,
                buildJsonObject { put("room_id", roomId.value) }.toString(),
            ).getOrThrow()
            roomId
        }
    }.onFailure { Timber.e(it, "Failed to open private journal") }

    /** Forgets the journal bookmark and account data so the next open creates a fresh encrypted room. */
    suspend fun reset(): Result<Unit> = runCatchingExceptions {
        mutex.withLock {
            pendingRoom = null
            withContext(dispatchers.io) {
                bookmark.delete()
            }
            client.setAccountData(ACCOUNT_DATA_TYPE, "{}").getOrThrow()
        }
    }

    private suspend fun validateExistingRoom(roomId: RoomId) {
        // Allow a generous window for a slow sync to catch up; a longer-than-usual pause is better
        // than a false "not synced" failure, and network recovery retries anyway.
        val info = withTimeoutOrNull(45_000) {
            client.getRoomInfoFlow(roomId).first { it.isPresent && it.get().isEncrypted != null }.get()
        } ?: run {
            Timber.e("Journal room $roomId has not synced yet after 45s")
            throw JournalOpenError.NotReady("The journal has not synced yet")
        }
        Timber.d("Journal room $roomId membership=${info.currentUserMembership} encrypted=${info.isEncrypted}")
        if (info.currentUserMembership != CurrentUserMembership.JOINED || info.isEncrypted != true) {
            throw JournalOpenError.InvalidState("The journal must be a joined encrypted room")
        }
    }

    /** Returns the room ID only when it looks like a valid Matrix room ID; otherwise null. */
    private fun parseRoomId(value: String): RoomId? =
        value.takeIf { it.startsWith('!') && ':' in it }?.let(::RoomId)

    /** Parses the room ID out of the raw account data JSON, tolerating empty or malformed payloads. */
    private fun parseAccountDataRoomId(raw: String?): RoomId? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            Json.parseToJsonElement(raw).jsonObject["room_id"]?.jsonPrimitive?.content
        }.getOrNull()?.let(::parseRoomId)
    }

    companion object {
        const val ACCOUNT_DATA_TYPE = "io.element.private.journal"
    }
}
