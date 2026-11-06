/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.motion

/** Identity aliases survive local-echo acknowledgement; historical insertions never enter the queue. */
internal data class MessageEntryCandidate(
    val id: String,
    val remoteId: String? = null,
    val transactionId: String? = null,
    val origin: Origin,
) {
    enum class Origin { Local, Sync, History }

    fun aliases(): List<String> = buildList {
        add("id:$id")
        remoteId?.let { add("event:$it") }
        transactionId?.let { add("transaction:$it") }
    }
}

/**
 * View-local admission, independent of rendering and the Matrix send pipeline. The initial snapshot
 * is a baseline, including on screen re-entry. Pending entries expire rather than animating later
 * when the user scrolls back to them. Only metadata is kept, never message text or media.
 */
internal class MessageEntryLedger(initial: List<MessageEntryCandidate>, live: Boolean) {
    private var committed = MessageEntryFrame(
        aliases = initial.flatMap { it.aliases() }.toSet(),
        awaitingFirstRemoteBatch = initial.isEmpty(),
        live = live,
        pending = mutableMapOf(),
    )

    /** Pure preparation: abandoned Compose work must not consume admission or replace the baseline. */
    fun prepare(events: List<MessageEntryCandidate>, isLive: Boolean, animate: Boolean, nowMillis: Long): MessageEntryFrame {
        val previous = committed
        val currentIds = events.map { it.id }.toSet()
        val pending = previous.pending.filter { it.value >= nowMillis && it.key in currentIds }.toMutableMap()
        if (!animate || !isLive || !previous.live) pending.clear()
        if (animate && isLive && previous.live) {
            // A reverse timeline prepends new messages. Appended pagination and replacement windows
            // must not animate, even if an SDK reports those events with a retained SYNC origin.
            val head = events.takeWhile { event -> event.aliases().none(previous.aliases::contains) }
            val hasAnchor = head.size < events.size || previous.aliases.isEmpty()
            if (hasAnchor) {
                head.forEach { event ->
                    val eligible = event.origin == MessageEntryCandidate.Origin.Local ||
                        (event.origin == MessageEntryCandidate.Origin.Sync && !previous.awaitingFirstRemoteBatch)
                    if (eligible) pending[event.id] = nowMillis + 1_500L
                }
            }
        }
        return MessageEntryFrame(
            aliases = events.flatMap { it.aliases() }.toSet(),
            awaitingFirstRemoteBatch = previous.awaitingFirstRemoteBatch && events.isEmpty(),
            live = isLive,
            pending = pending,
        )
    }

    fun commit(frame: MessageEntryFrame) { committed = frame }

    // Explicit non-Compose entry points also exercise the exact preparation/commit path in JVM tests.
    fun update(events: List<MessageEntryCandidate>, isLive: Boolean, animate: Boolean, nowMillis: Long) {
        commit(prepare(events, isLive, animate, nowMillis))
    }

    fun claim(id: String, nowMillis: Long): Boolean = committed.claim(id, nowMillis)
}

internal class MessageEntryFrame(
    val aliases: Set<String>,
    val awaitingFirstRemoteBatch: Boolean,
    val live: Boolean,
    internal val pending: MutableMap<String, Long>,
) {
    fun isEligible(id: String, nowMillis: Long): Boolean = pending[id]?.let { it >= nowMillis } ?: false
    fun claim(id: String, nowMillis: Long): Boolean = pending.remove(id)?.let { it >= nowMillis } ?: false
}
