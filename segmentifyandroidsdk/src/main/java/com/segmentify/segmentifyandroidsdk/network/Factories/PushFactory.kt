package com.segmentify.segmentifyandroidsdk.network.Factories

import com.segmentify.segmentifyandroidsdk.model.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PushFactory {

        // Gimli returns text/plain (not JSON) on success/error.
        @POST("/native/subscription/push")
        fun sendNotification(@Body notificationModel: NotificationModel): Call<ResponseBody>

        @POST("/native/interaction/notification")
        fun sendNotificationInteraction(@Body notificationModel: NotificationModel): Call<ResponseBody>

}
