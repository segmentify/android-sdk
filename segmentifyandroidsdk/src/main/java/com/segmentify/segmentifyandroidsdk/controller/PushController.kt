package com.segmentify.segmentifyandroidsdk.controller

import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.model.NotificationModel
import com.segmentify.segmentifyandroidsdk.network.ConnectionManager
import com.segmentify.segmentifyandroidsdk.network.Factories.PushFactory
import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger
import okhttp3.ResponseBody
import retrofit2.Call


internal object PushController {

    fun sendNotification(notificationModel: NotificationModel) {
        send("notification model") { push, apiKey -> push.sendNotification(notificationModel, apiKey) }
    }

    fun sendNotificationInteraction(notificationModel: NotificationModel) {
        send("notification interaction model") { push, apiKey -> push.sendNotificationInteraction(notificationModel, apiKey) }
    }

    /**
     * Push calls have no callback, so a call that cannot be made is logged and
     * dropped. They can come from a messaging service the system started
     * before the app configured the SDK.
     */
    private fun send(what: String, call: (PushFactory, String) -> Call<ResponseBody>) {
        val apiKey = SegmentifyManager.configModel.apiKey
        if (SegmentifyManager.clientPreferences == null || apiKey.isNullOrBlank()) {
            SegmentifyLogger.printErrorLog("Could not send $what: " + ConnectionManager.NOT_CONFIGURED)
            return
        }
        try {
            call(ConnectionManager.getPushFactory(), apiKey).enqueue(object : NetworkCallback<ResponseBody>() {
                override fun onSuccess(response: ResponseBody) {
                    response.close()
                }
            })
        } catch (e: Exception) {
            SegmentifyLogger.printErrorLog("Error occurred when sending $what, error: $e")
        }
    }
}
