package com.segmentify.segmentifyandroidsdk.model.faceted

import com.segmentify.segmentifyandroidsdk.model.BackendResponse

class SearchFacetedEventResponseModel : BackendResponse {
    override var statusCode : String? = null
    var search: ArrayList<ArrayList<SearchFacetedResponseModel>>? = null
}