package com.segmentify.segmentifyandroidsdk.model

import android.os.Build
import com.segmentify.segmentifyandroidsdk.SegmentifyManager

class NotificationModel{

    var deviceToken:String? = null
    var type:NotificationType? = null
    var instanceId:String? = null
    var interactionId:String? = null
    private  var userId : String?=null
    private  var osVersion:String?=null
    private  var os:String?=null
    private  var providerType:String?=null


    init {
        if(type != NotificationType.PERMISSION_INFO){
            deviceToken = null
        }

        osVersion = Build.VERSION.SDK_INT.toString()
        os = "ANDROID"
        providerType = "FIREBASE"

        var userId_ = SegmentifyManager.clientPreferences?.getUserId()

        if(!userId_.isNullOrEmpty()){
            userId = userId_
        }

    }

}