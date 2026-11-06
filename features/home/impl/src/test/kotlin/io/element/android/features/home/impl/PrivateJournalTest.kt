/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.paths.SessionPaths
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.aRoomInfo
import io.element.android.tests.testutils.testCoroutineDispatchers
import java.io.IOException
import java.util.Optional
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrivateJournalTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `explicit open creates an encrypted private room and saves its account bookmark`() = runTest {
        var parameters: CreateRoomParameters? = null
        var savedContent: String? = null
        val client = FakeMatrixClient(
            sessionPaths = paths(),
            getAccountDataLambda = { Result.success(null) },
            setAccountDataLambda = { type, content ->
                assertThat(type).isEqualTo(PrivateJournal.ACCOUNT_DATA_TYPE)
                savedContent = content
                Result.success(Unit)
            },
        ).apply {
            createRoomResult = { parameters = it; Result.success(A_ROOM_ID) }
        }

        assertThat(PrivateJournal(client, testCoroutineDispatchers()).open().getOrThrow()).isEqualTo(A_ROOM_ID)
        assertThat(parameters?.isEncrypted).isTrue()
        assertThat(parameters?.visibility).isEqualTo(RoomVisibility.Private)
        assertThat(parameters?.preset).isEqualTo(RoomPreset.PRIVATE_CHAT)
        assertThat(parameters?.invite).isEmpty()
        assertThat(Json.parseToJsonElement(checkNotNull(savedContent)).jsonObject.getValue("room_id").jsonPrimitive.content)
            .isEqualTo(A_ROOM_ID.value)
    }

    @Test
    fun `failed account bookmark upload retries the same room after service recreation`() = runTest {
        var creates = 0
        var writes = 0
        val client = FakeMatrixClient(
            sessionPaths = paths(),
            getAccountDataLambda = { Result.success(null) },
            setAccountDataLambda = { _, _ ->
                writes++
                if (writes == 1) Result.failure(IOException("offline")) else Result.success(Unit)
            },
        ).apply {
            createRoomResult = { creates++; Result.success(A_ROOM_ID) }
            getRoomInfoFlowLambda = { flowOf(Optional.of(aRoomInfo(id = A_ROOM_ID, isEncrypted = true))) }
        }
        val dispatchers = testCoroutineDispatchers()

        assertThat(PrivateJournal(client, dispatchers).open().isFailure).isTrue()
        assertThat(PrivateJournal(client, dispatchers).open().getOrThrow()).isEqualTo(A_ROOM_ID)
        assertThat(creates).isEqualTo(1)
        assertThat(writes).isEqualTo(2)
    }

    @Test
    fun `failed bookmark lookup never creates a replacement room`() = runTest {
        var creates = 0
        val client = FakeMatrixClient(
            sessionPaths = paths(),
            getAccountDataLambda = { Result.failure(IOException("offline")) },
        ).apply {
            createRoomResult = { creates++; Result.success(A_ROOM_ID) }
        }

        assertThat(PrivateJournal(client, testCoroutineDispatchers()).open().isFailure).isTrue()
        assertThat(creates).isEqualTo(0)
    }

    @Test
    fun `unencrypted bookmark is rejected without creating or opening a room`() = runTest {
        var creates = 0
        val client = FakeMatrixClient(
            sessionPaths = paths(),
            getAccountDataLambda = { Result.success("""{"room_id":"${A_ROOM_ID.value}"}""") },
        ).apply {
            createRoomResult = { creates++; Result.success(A_ROOM_ID) }
            getRoomInfoFlowLambda = { flowOf(Optional.of(aRoomInfo(id = A_ROOM_ID, isEncrypted = false))) }
        }

        assertThat(PrivateJournal(client, testCoroutineDispatchers()).open().isFailure).isTrue()
        assertThat(creates).isEqualTo(0)
    }

    private fun paths() = SessionPaths(fileDirectory = temporaryFolder.newFolder(), cacheDirectory = temporaryFolder.newFolder())
}
