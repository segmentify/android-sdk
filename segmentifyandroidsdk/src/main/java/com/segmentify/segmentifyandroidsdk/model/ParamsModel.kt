package com.segmentify.segmentifyandroidsdk.model

import com.google.gson.annotations.SerializedName

class ParamsModel {
    var notificationTitle : String? = null

    @SerializedName("recommendedProducts")
    var recommendedProducts : Object? = null

    var dynamicItems : String? = null
    var instanceId : String? = null
    var actionId : String? = null
}