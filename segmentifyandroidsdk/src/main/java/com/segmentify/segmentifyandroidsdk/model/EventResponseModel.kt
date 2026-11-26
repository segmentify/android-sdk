package com.segmentify.segmentifyandroidsdk.model

class EventResponseModel : BackendResponse {
    override var statusCode : String? = null
    var responses : ArrayList<ArrayList<ResponseModel>>? = null
}