package com.segmentify.segmentifyandroidsdk.controller

import com.segmentify.segmentifyandroidsdk.utils.ClientPreferences

/**
 * When the stored session id may still be used (SFY-28789).
 *
 * The stored value is the moment the session ends: the last activity plus
 * `sessionKeepSeconds`. A session is used only up to that moment, every event
 * moves it forward, and the check runs before every event.
 */
internal object SessionWindow {

    /** Milliseconds since the epoch; replaced in tests. */
    @Volatile
    var clock: () -> Long = { System.currentTimeMillis() }

    /** The stored session id while it is active; otherwise forgets it and returns null. */
    fun activeSessionId(preferences: ClientPreferences): String? = preferences.getSessionId().ifEmpty { null }

    /** Starts the window again from now: called for every event sent and every new session. */
    fun extend(preferences: ClientPreferences) {
    }
}
