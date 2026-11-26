package com.segmentify.segmentifyandroidsdk.controller

import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.network.ConnectionManager
import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger


internal object KeyController {

    /**
     * Forgets the stored session id when its window has passed, so that the next
     * event asks for a new one. Also runs before every event; see [SessionWindow].
     */
    fun controlSessionTimeout() {
        SegmentifyManager.clientPreferences?.let { SessionWindow.activeSessionId(it) }
    }

    /** Events that arrived before the ids; see [PendingEventQueue]. */
    val pendingEvents = PendingEventQueue(
        ids = { currentIds() },
        requestIds = { requestMissingIds() },
        log = { SegmentifyLogger.printErrorLog(it) }
    )

    private fun currentIds(): SessionIds? {
        val preferences = SegmentifyManager.clientPreferences ?: return null
        val userId = preferences.getUserId()
        val sessionId = preferences.getSessionId()
        return if (userId.isEmpty() || sessionId.isEmpty()) null else SessionIds(userId, sessionId)
    }

    private fun requestMissingIds() {
        val preferences = SegmentifyManager.clientPreferences
        if (preferences == null) {
            val notConfigured = SegmentifyException("SegmentifyManager.config must be called before sending events")
            SegmentifyLogger.printErrorLog(notConfigured.message.toString())
            pendingEvents.onIdsFailed(notConfigured)
            return
        }
        try {
            if (preferences.getUserId().isEmpty()) {
                getUserIdAndSessionId()
            } else {
                getSessionId()
            }
        } catch (t: Throwable) {
            SegmentifyLogger.printErrorLog("Could not request user id and session id: " + t.message)
            pendingEvents.onIdsFailed(t)
        }
    }

    fun getSessionId() {
        ConnectionManager.getUserSessionFactory().getSession(1)
            .enqueue(object : NetworkCallback<ArrayList<String>>() {
                override fun onSuccess(response: ArrayList<String>) {
                    val sessionId = response.getOrNull(0)
                    if (sessionId.isNullOrEmpty()) {
                        pendingEvents.onIdsFailed(SegmentifyException("The key response held no session id"))
                        return
                    }
                    SegmentifyManager.clientPreferences?.setSessionId(sessionId)
                    SegmentifyManager.clientPreferences?.let { SessionWindow.extend(it) }
                    pendingEvents.onIdsAvailable()
                }

                override fun onGiveUp(error: SegmentifyException) {
                    pendingEvents.onIdsFailed(error)
                }
            })
    }

    fun getUserIdAndSessionId() {
        ConnectionManager.getUserSessionFactory().getUserIdAndSession(2)
            .enqueue(object : NetworkCallback<ArrayList<String>>() {
                override fun onSuccess(response: ArrayList<String>) {
                    val userId = response.getOrNull(0)
                    val sessionId = response.getOrNull(1)
                    if (userId.isNullOrEmpty() || sessionId.isNullOrEmpty()) {
                        pendingEvents.onIdsFailed(SegmentifyException("The key response held no user id and session id"))
                        return
                    }
                    SegmentifyManager.clientPreferences?.setUserId(userId)
                    SegmentifyManager.clientPreferences?.setSessionId(sessionId)
                    SegmentifyManager.clientPreferences?.let { SessionWindow.extend(it) }
                    pendingEvents.onIdsAvailable()
                }

                override fun onGiveUp(error: SegmentifyException) {
                    pendingEvents.onIdsFailed(error)
                }
            })
    }

}
