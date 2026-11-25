package com.segmentify.segmentifyandroidsdk.controller

internal data class SessionIds(val userId: String, val sessionId: String)

/**
 * Decides what happens to an event when the user id and session id are not
 * known yet. Free of Android and networking so it can be tested on the JVM.
 *
 * @param ids the current ids, or null while they are missing
 * @param requestIds starts fetching the missing ids; the owner reports the
 *   outcome through [onIdsAvailable] or [onIdsFailed]
 */
internal class PendingEventQueue(
    private val ids: () -> SessionIds?,
    private val requestIds: () -> Unit,
    private val log: (String) -> Unit = {},
    private val maxPending: Int = DEFAULT_MAX_PENDING
) {

    private val lock = Any()
    private val pending = ArrayDeque<(SessionIds) -> Unit>()
    private var requesting = false

    /**
     * Runs [event] with the ids. When they are not known yet the event waits
     * (the oldest one is dropped once [maxPending] are waiting) and the ids
     * are requested, unless a request is already running.
     */
    fun submit(event: (SessionIds) -> Unit) {
        val current = ids()
        if (current != null) {
            event(current)
            return
        }

        synchronized(lock) {
            if (pending.size >= maxPending) {
                pending.removeFirst()
                log("Dropped the oldest waiting event: $maxPending events are waiting for a user id and session id")
            }
            pending.addLast(event)
        }
        requestIdsIfIdle()
    }

    /** Requests the ids when they are missing and no request is running. */
    fun ensureIds() {
        if (ids() == null) {
            requestIdsIfIdle()
        }
    }

    /** The owner stored new ids: send everything that was waiting, in order. */
    fun onIdsAvailable() {
        val current = ids()
        val ready: List<(SessionIds) -> Unit> = synchronized(lock) {
            requesting = false
            if (current == null) {
                emptyList()
            } else {
                pending.toList().also { pending.clear() }
            }
        }
        if (current != null) {
            ready.forEach { it(current) }
        }
    }

    /** The id request failed: keep the events; the next event or [ensureIds] retries. */
    fun onIdsFailed() {
        synchronized(lock) {
            requesting = false
        }
    }

    private fun requestIdsIfIdle() {
        synchronized(lock) {
            if (requesting) {
                return
            }
            requesting = true
        }
        requestIds()
    }

    companion object {
        const val DEFAULT_MAX_PENDING = 50
    }
}
