package com.segmentify.segmentifyandroidsdk.controller

import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.model.NotificationModel
import com.segmentify.segmentifyandroidsdk.network.ConnectionManager
import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import com.segmentify.segmentifyandroidsdk.utils.ClientPreferences
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger
import okhttp3.ResponseBody
import java.lang.Exception


internal object PushController {

    var clientPreferences: ClientPreferences? = null

    fun sendNotification(notificationModel: NotificationModel, callback: SegmentifyCallback<Boolean>? = null) {

        try {
            ConnectionManager.getPushFactory().sendNotification(notificationModel)
                    .enqueue(object : NetworkCallback<ResponseBody>() {
                        override fun onSuccess(response: ResponseBody) {
                            response.close()
                            callback?.onDataLoaded(true)
                        }

                        override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                            super.onFailure(call, t)
                            callback?.onDataLoaded(false)
                        }
                    })
        } catch (e: Exception) {
            SegmentifyLogger.printErrorLog("Error occurred when sending notification model, error: $e")
            callback?.onDataLoaded(false)
        }
    }

    fun sendNotificationInteraction(notificationModel: NotificationModel) {

        try {
            ConnectionManager.getPushFactory().sendNotificationInteraction(notificationModel)
                    .enqueue(object : NetworkCallback<ResponseBody>() {
                        override fun onSuccess(response: ResponseBody) {
                            response.close()
                        }
                    })
        } catch (e: Exception) {
            SegmentifyLogger.printErrorLog("Error occurred when sending notification interaction model, error: $e")
        }
    }
}