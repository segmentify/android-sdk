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
    private class Waiting(val event: (SessionIds) -> Unit, val onDropped: ((cause: Throwable?) -> Unit)?)

    private val pending = ArrayDeque<Waiting>()
    private var requesting = false

    /**
     * Runs [event] with the ids. When they are not known yet the event waits
     * (the oldest one is dropped once [maxPending] are waiting) and the ids
     * are requested, unless a request is already running.
     */
    fun submit(event: (SessionIds) -> Unit) {
        submit(event, null)
    }

    /**
     * Like [submit]; [onDropped] is called if the event is given up before it
     * could run, with the reason the ids could not be obtained when there is one.
     */
    fun submit(event: (SessionIds) -> Unit, onDropped: ((cause: Throwable?) -> Unit)?) {
        val current = ids()
        if (current != null) {
            event(current)
            return
        }

        // Read the ids again under the lock: they may have arrived, and
        // onIdsAvailable() may already have run, since the read above. Enqueueing
        // then would start a second request and replace a fresh session id.
        var dropped: Waiting? = null
        val arrivedMeanwhile = synchronized(lock) {
            val latest = ids()
            if (latest == null) {
                if (pending.size >= maxPending) {
                    dropped = pending.removeFirst()
                    log("Dropped the oldest waiting event: $maxPending events are waiting for a user id and session id")
                }
                pending.addLast(Waiting(event, onDropped))
            }
            latest
        }
        dropped?.onDropped?.invoke(null)
        if (arrivedMeanwhile != null) {
            event(arrivedMeanwhile)
            return
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
        val ready: List<Waiting> = synchronized(lock) {
            requesting = false
            if (current == null) {
                emptyList()
            } else {
                pending.toList().also { pending.clear() }
            }
        }
        if (current != null) {
            // This runs inside the key request's callback: one failing event must
            // neither stop the others nor escape into the HTTP client's thread.
            for (waiting in ready) {
                try {
                    waiting.event(current)
                } catch (e: Exception) {
                    log("A waiting event could not be sent: " + e.message)
                }
            }
        }
    }

    /**
     * The id request failed. Events whose caller can be told are given up and
     * told so; the others stay, and the next event or [ensureIds] retries.
     */
    fun onIdsFailed(cause: Throwable? = null) {
        val givenUp: List<Waiting> = synchronized(lock) {
            requesting = false
            val answerable = pending.filter { it.onDropped != null }
            pending.removeAll(answerable)
            answerable
        }
        for (waiting in givenUp) {
            try {
                waiting.onDropped?.invoke(null)
            } catch (e: Exception) {
                log("A waiting event could not be told that it was given up: " + e.message)
            }
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
