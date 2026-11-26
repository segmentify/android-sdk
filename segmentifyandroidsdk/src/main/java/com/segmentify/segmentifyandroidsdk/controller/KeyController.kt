package com.segmentify.segmentifyandroidsdk.controller

import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.network.ConnectionManager
import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger
import java.util.*
import java.util.concurrent.TimeUnit


internal object KeyController {

    fun controlSessionTimeout() {
        val calendar = Calendar.getInstance()
        calendar.time = Calendar.getInstance().time
        calendar.add(
            Calendar.SECOND,
            SegmentifyManager.clientPreferences?.getSessionKeepSeconds()!!
        )
        var date = calendar.timeInMillis
        if (SegmentifyManager.clientPreferences?.getSavedDateForSession() != null) {
            date = SegmentifyManager.clientPreferences?.getSavedDateForSession()!!
        }
        val dayBySecondsDifference = getDifferenceDays(date, Calendar.getInstance().timeInMillis)
        if (dayBySecondsDifference > SegmentifyManager.clientPreferences?.getSessionKeepSeconds()!!) {
            SegmentifyManager.clientPreferences?.setSavedDateforSession(calendar.timeInMillis)
            // Expired (or never stored): forget the session id. The caller then
            // asks pendingEvents.ensureIds() for a new one, so there is a single
            // key request and events sent meanwhile wait for it.
            SegmentifyManager.clientPreferences?.setSessionId("")
        } else {
            SegmentifyManager.clientPreferences?.setSavedDateforSession(calendar.timeInMillis)
        }
    }

    fun getDifferenceDays(d1: Long, d2: Long): Long {
        val diff = d2 - d1
        return TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS)
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
            val notConfigured = SegmentifyException(ConnectionManager.NOT_CONFIGURED)
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
                    pendingEvents.onIdsAvailable()
                }

                override fun onGiveUp(error: SegmentifyException) {
                    pendingEvents.onIdsFailed(error)
                }
            })
    }

}
