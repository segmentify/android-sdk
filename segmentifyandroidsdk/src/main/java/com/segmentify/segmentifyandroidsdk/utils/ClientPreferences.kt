package com.segmentify.segmentifyandroidsdk.utils

import android.content.Context

class ClientPreferences(context: Context) : PreferencesManager(context) {


    private val USER_NAME = "USERNAME"
    private val EMAIL = "EMAIL"
    private val USER_ID = "USER_ID"
    private val SESSION_ID = "SESSION_ID"
    private val IS_LOG_VISIBLE = "IS_LOG_VISIBLE"
    private val API_URL = "API_URL"
    private val SAVED_DATE_FOR_SESSION_MILLIS = "SAVED_DATE_FOR_SESSION_MILLIS"
    private val SESSION_KEEP_SECONDS = "SESSION_KEEP_SECONDS"
    private val PUSH_CAMPAIGN_ID = "PUSH_CAMPAIGN_ID"
    private val PUSH_CAMPAIGN_PRODUCT_ID = "PUSH_CAMPAIGN_PRODUCT_ID"
    private val USER_PROFILE_SNAPSHOT = "SEG_USER_PROFILE"
    private val DEVICE_TOKEN = "DEVICE_TOKEN"
    private val AUTH_TOKEN = "AUTH_TOKEN"

    fun getApiUrl(): String {
        return getString(API_URL, "")!!
    }

    fun setApiUrl(apiUrl: String) {
        putString(API_URL, apiUrl)
    }

    fun getUserId(): String {
        return getString(USER_ID, "")!!
    }

    fun setUserId(userId: String) {
        putString(USER_ID, userId)
    }

    fun getSessionId(): String {
        return getString(SESSION_ID, "")!!
    }

    fun setSessionId(userId: String) {
        putString(SESSION_ID, userId)
    }

    fun isLogVisible(): Boolean {
        return getBooleanValue(IS_LOG_VISIBLE, false)
    }

    fun setLogVisible(isLogVisible: Boolean) {
        putBoolean(IS_LOG_VISIBLE, isLogVisible)
    }

    fun getSavedDateForSession(): Long? {
        return getLong(SAVED_DATE_FOR_SESSION_MILLIS)
    }

    fun setSavedDateforSession(date: Long) {
        putLong(SAVED_DATE_FOR_SESSION_MILLIS, date)
    }

    fun getSessionKeepSeconds(): Int {
        return getInt(SESSION_KEEP_SECONDS, 86400)
    }

    fun setSessionKeepSeconds(sessionKeepSeconds: Int) {
        putInt(SESSION_KEEP_SECONDS, sessionKeepSeconds)
    }

    fun setEmail(email: String) {
        putString(EMAIL, email)
    }

    fun setUserName(userName: String) {
        putString(USER_NAME, userName)
    }

    fun setPushCampaignId(instanceId: String) {
        putString(PUSH_CAMPAIGN_ID, instanceId)
    }

    fun setPushCampaignProductId(productId: String) {
        putString(PUSH_CAMPAIGN_PRODUCT_ID, productId)
    }

    fun getUserProfileSnapshot(): String? {
        val snapshot = getString(USER_PROFILE_SNAPSHOT, "")
        return if (snapshot.isNullOrBlank()) null else snapshot
    }

    fun setUserProfileSnapshot(snapshot: String) {
        putString(USER_PROFILE_SNAPSHOT, snapshot)
    }

    fun clearUserProfileSnapshot() {
        clearKey(USER_PROFILE_SNAPSHOT)
    }

    fun getDeviceToken(): String {
        return getString(DEVICE_TOKEN, "")!!
    }

    fun setDeviceToken(deviceToken: String) {
        putString(DEVICE_TOKEN, deviceToken)
    }

    fun getAuthToken(): String? {
        val token = getString(AUTH_TOKEN, "")
        return if (token.isNullOrBlank()) null else token
    }

    fun setAuthToken(authToken: String) {
        putString(AUTH_TOKEN, authToken)
    }

}