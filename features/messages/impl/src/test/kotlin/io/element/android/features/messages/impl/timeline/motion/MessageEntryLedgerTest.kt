/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.motion

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.motion.MessageEntryCandidate.Origin
import org.junit.Test

class MessageEntryLedgerTest {
    private val history = MessageEntryCandidate("existing", origin = Origin.Sync)

    @Test
    fun `initial snapshot and reopening never replay messages`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        ledger.update(listOf(history), isLive = true, animate = true, nowMillis = 0L)
        assertThat(ledger.claim(history.id, 0L)).isFalse()
    }

    @Test
    fun `every new live head message enters once`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val first = MessageEntryCandidate("first", origin = Origin.Local)
        val second = MessageEntryCandidate("second", origin = Origin.Sync)
        ledger.update(listOf(second, first, history), true, true, 10L)
        assertThat(ledger.claim(first.id, 10L)).isTrue()
        assertThat(ledger.claim(second.id, 10L)).isTrue()
        assertThat(ledger.claim(first.id, 10L)).isFalse()
    }

    @Test
    fun `historical pagination cannot enter even with a retained sync origin`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val old = MessageEntryCandidate("old", origin = Origin.Sync)
        ledger.update(listOf(history, old), true, true, 10L)
        assertThat(ledger.claim(old.id, 10L)).isFalse()
    }

    @Test
    fun `local acknowledgement does not replay with changed unique and remote ids`() {
        val local = MessageEntryCandidate("local", transactionId = "transaction", origin = Origin.Local)
        val ledger = MessageEntryLedger(listOf(history), live = true)
        ledger.update(listOf(local, history), true, true, 10L)
        assertThat(ledger.claim(local.id, 10L)).isTrue()
        val remote = local.copy(id = "remote", remoteId = "event", origin = Origin.Sync)
        ledger.update(listOf(remote, history), true, true, 20L)
        assertThat(ledger.claim(remote.id, 20L)).isFalse()
    }

    @Test
    fun `first remote sync after an empty loading state is a baseline`() {
        val ledger = MessageEntryLedger(emptyList(), live = true)
        ledger.update(listOf(history), true, true, 10L)
        assertThat(ledger.claim(history.id, 10L)).isFalse()
        val received = MessageEntryCandidate("received", origin = Origin.Sync)
        ledger.update(listOf(received, history), true, true, 20L)
        assertThat(ledger.claim(received.id, 20L)).isTrue()
    }

    @Test
    fun `first local send in an empty room animates`() {
        val local = MessageEntryCandidate("local", origin = Origin.Local)
        val ledger = MessageEntryLedger(emptyList(), live = true)
        ledger.update(listOf(local), true, true, 10L)
        assertThat(ledger.claim(local.id, 10L)).isTrue()
    }

    @Test
    fun `focus and jump to live seed their new windows`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val focused = MessageEntryCandidate("focused", origin = Origin.Sync)
        ledger.update(listOf(focused), false, true, 10L)
        assertThat(ledger.claim(focused.id, 10L)).isFalse()
        val latest = MessageEntryCandidate("latest", origin = Origin.Sync)
        ledger.update(listOf(latest), true, true, 20L)
        assertThat(ledger.claim(latest.id, 20L)).isFalse()
    }

    @Test
    fun `replaced timeline without known anchor does not animate an entire window`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val replacement = MessageEntryCandidate("replacement", origin = Origin.Sync)
        ledger.update(listOf(replacement), true, true, 10L)
        assertThat(ledger.claim(replacement.id, 10L)).isFalse()
    }

    @Test
    fun `offscreen messages expire instead of playing on scroll back`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val recent = MessageEntryCandidate("recent", origin = Origin.Sync)
        ledger.update(listOf(recent, history), true, true, 10L)
        assertThat(ledger.claim(recent.id, 1511L)).isFalse()
    }

    @Test
    fun `disabled animation still records the baseline`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val recent = MessageEntryCandidate("recent", origin = Origin.Sync)
        ledger.update(listOf(recent, history), true, false, 10L)
        ledger.update(listOf(recent, history), true, true, 20L)
        assertThat(ledger.claim(recent.id, 20L)).isFalse()
    }

    @Test
    fun `pending entry is cancelled when leaving the live timeline`() {
        val ledger = MessageEntryLedger(listOf(history), live = true)
        val recent = MessageEntryCandidate("recent", origin = Origin.Sync)
        ledger.update(listOf(recent, history), true, true, 10L)
        ledger.update(listOf(recent), false, true, 20L)
        assertThat(ledger.claim(recent.id, 20L)).isFalse()
    }

}
