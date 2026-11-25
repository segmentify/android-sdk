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

    /** Runs [event] with the ids. Current behaviour: an event without ids is dropped. */
    fun submit(event: (SessionIds) -> Unit) {
        val current = ids()
        if (current != null) {
            event(current)
        } else {
            log("You must fill userid&sessionid before sending an event")
        }
    }

    fun ensureIds() {
        if (ids() == null) {
            requestIds()
        }
    }

    fun onIdsAvailable() {
    }

    fun onIdsFailed() {
    }

    companion object {
        const val DEFAULT_MAX_PENDING = 50
    }
}
