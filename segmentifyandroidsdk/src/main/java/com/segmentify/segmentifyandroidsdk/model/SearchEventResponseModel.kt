package com.segmentify.segmentifyandroidsdk.model

class SearchEventResponseModel : BackendResponse {
    override var statusCode : String? = null
    var search : ArrayList<ArrayList<SearchResponseModel>>? = null

}