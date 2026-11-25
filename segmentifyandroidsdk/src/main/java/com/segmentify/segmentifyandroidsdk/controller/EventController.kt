package com.segmentify.segmentifyandroidsdk.controller

import com.google.gson.internal.LinkedTreeMap
import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.model.*
import com.segmentify.segmentifyandroidsdk.model.faceted.SearchFacetedEventResponseModel
import com.segmentify.segmentifyandroidsdk.model.faceted.SearchFacetedPageModel
import com.segmentify.segmentifyandroidsdk.model.faceted.SearchFacetedResponseModel
import com.segmentify.segmentifyandroidsdk.network.ConnectionManager
import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import com.segmentify.segmentifyandroidsdk.utils.CdpUserProfileStore
import com.segmentify.segmentifyandroidsdk.utils.Constant
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger

internal object EventController {

    fun sendPageView(pageModel : PageModel,segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>){

        if(!pageModel.userId.isNullOrEmpty() && !pageModel.sessionId.isNullOrEmpty()){

            ConnectionManager.getEventFactory().sendPageView(pageModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>(){
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }
                    })
        }

        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing pageview event")
            return
        }

    }

    fun sendSearchView(pageModel: SearchPageModel, segmentifyCallback: SegmentifyCallback<SearchResponseModel>){
        if(!pageModel.userId.isNullOrEmpty() && !pageModel.sessionId.isNullOrEmpty()){
            ConnectionManager.getEventFactory().sendSearchView(pageModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<SearchEventResponseModel>(){
                        override fun onSuccess(response: SearchEventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatSearchResponse(response))
                        }
                    })
        }

        else{
            SegmentifyLogger.printErrorLog("You must fill userid & sessionid before accessing pageview event")
            return
        }
    }

    fun reformatSearchResponse(response: SearchEventResponseModel): SearchResponseModel {
        var returnVal = SearchResponseModel()
        if (response.search != null && response.search!!.isNotEmpty()) {
            var list = response.search?.get(0)
            if (list != null && list.isEmpty().not()) {
                returnVal = list[0]
                SegmentifyManager.sendWidgetView("SEARCH", "static")
                SegmentifyManager.sendImpression("SEARCH", "static")
            }
        }
        return returnVal
    }

    fun sendFacetedSearchView(pageModel: SearchFacetedPageModel, segmentifyCallback: SegmentifyCallback<SearchFacetedResponseModel>){
        if (!pageModel.userId.isNullOrEmpty() && !pageModel.sessionId.isNullOrEmpty()) {
            ConnectionManager.getEventFactory()
                .sendFacetedSearchView(pageModel, SegmentifyManager.configModel.apiKey!!)
                .enqueue(object : NetworkCallback<SearchFacetedEventResponseModel>() {
                    override fun onSuccess(response: SearchFacetedEventResponseModel) {
                        segmentifyCallback.onDataLoaded(reformatSearchFacetedResponse(response))
                    }
                })
        } else {
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing pageview event")
            return
        }
    }

    fun reformatSearchFacetedResponse(response: SearchFacetedEventResponseModel): SearchFacetedResponseModel {
        var returnVal = SearchFacetedResponseModel()
        var list = response.search?.get(0)
        if(list != null && list.isEmpty().not()){
            returnVal = list[0]
        }

        return returnVal
    }

    fun sendCustomEvent(customEventModel: CustomEventModel, segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>) {

        if(!customEventModel.userId.isNullOrEmpty() && !customEventModel.sessionId.isNullOrEmpty()){


            ConnectionManager.getEventFactory().sendCustomEvent(customEventModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>() {
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing custom event")
            return
        }


    }

    fun sendProductView(productModel: ProductModel, segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>){


        if(!productModel.userId.isNullOrEmpty() && !productModel.sessionId.isNullOrEmpty()){


            ConnectionManager.getEventFactory().sendProductView(productModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>() {
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }
                    })
        }

        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing productview event")
            return
        }
    }

    fun sendAddOrRemoveBasket(basketModel: BasketModel){

        if(!basketModel.userId.isNullOrEmpty() && !basketModel.sessionId.isNullOrEmpty()){

            ConnectionManager.getEventFactory().sendAddOrRemoveBasket(basketModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing addorremove event")
            return
        }

    }

    fun sendCheckout(checkoutModel: CheckoutModel,segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>) {

        if(!checkoutModel.userId.isNullOrEmpty() && !checkoutModel.sessionId.isNullOrEmpty()){
            ConnectionManager.getEventFactory().sendPurchase(checkoutModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>(){
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }
                    })
        }

        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing checkout event")
            return
        }

    }

    fun sendUserOperation(userModel: UserModel){

        if(!userModel.userId.isNullOrEmpty() && !userModel.sessionId.isNullOrEmpty()){

            ConnectionManager.getEventFactory().sendUserOperation(userModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing useroperation event")
            return
        }
    }

    fun sendUserTraits(userTraitsEventModel: UserTraitsEventModel) {

        if (!userTraitsEventModel.userId.isNullOrEmpty() && !userTraitsEventModel.sessionId.isNullOrEmpty()) {

            ConnectionManager.getEventFactory().sendUserTraits(userTraitsEventModel, SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>() {
                        override fun onSuccess(response: Any) {
                        }
                    })
        } else {
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing user traits event")
            return
        }
    }

    fun sendCdpUserEvent(model: UserTraitsEventModel, identityPayload: CdpEventsPayload? = null) {

        if (!model.userId.isNullOrEmpty() && !model.sessionId.isNullOrEmpty()) {

            ConnectionManager.getEventFactory().sendUserTraits(model, SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>() {
                        override fun onSuccess(response: Any) {
                            if (identityPayload != null) {
                                CdpUserProfileStore.default.markIdentifySent(identityPayload)
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<Any>, t: Throwable) {
                            super.onFailure(call, t)
                            if (identityPayload != null) {
                                CdpUserProfileStore.default.clearInFlightIdentify(identityPayload)
                            }
                        }
                    })
        } else {
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing cdp user event")
            return
        }
    }

    fun sendChangeUser(userChangeModel: UserChangeModel,segmentifyCallback : SegmentifyCallback<Boolean>){

        if(!userChangeModel.userId.isNullOrEmpty() && !userChangeModel.sessionId.isNullOrEmpty()){

            ConnectionManager.getEventFactory().sendChangeUser(userChangeModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                            segmentifyCallback.onDataLoaded(true)
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing changeuser event")
            return
        }


    }

    fun sendInteractionEvent(interactionModel: InteractionModel){

        if(!interactionModel.userId.isNullOrEmpty() && !interactionModel.sessionId.isNullOrEmpty()){
            ConnectionManager.getEventFactory().sendInteractionEvent(interactionModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing interaction event")
            return
        }

    }

    fun sendBannerOperations(bannerOperationOperationsModel: BannerOperationsModel){

        if(!bannerOperationOperationsModel.userId.isNullOrEmpty() && !bannerOperationOperationsModel.sessionId.isNullOrEmpty()){
            ConnectionManager.getEventFactory().sendBannerOperations(bannerOperationOperationsModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing banner operation event")
            return
        }

    }

    fun sendBannerGroupView(bannerGroupViewModel: BannerGroupViewModel){

        if(!bannerGroupViewModel.userId.isNullOrEmpty() && !bannerGroupViewModel.sessionId.isNullOrEmpty()){
            ConnectionManager.getEventFactory().sendBannerGroupView(bannerGroupViewModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                            try {
                                var resp = (response as LinkedTreeMap<String, Any>)
                                if ((resp.get("statusCode") as String).equals("SUCCESS")) {
                                    var responses = (resp.get("responses") as ArrayList<Any>)
                                    if (responses.size > 0) {
                                        var response = (responses.get(0) as ArrayList<Any>)
                                        if (response.size > 0) {
                                            var responseDetail = (response.get(0) as LinkedTreeMap<String, Any>)
                                            if ((responseDetail.get("type") as String).equals("sendBannerDetails")) {
                                                SegmentifyManager.sendInternalBannerGroupEvent(bannerGroupViewModel)
                                            }
                                        }
                                    }
                                }
                            } catch (err: Exception) {
                            }
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing banner group view event")
            return
        }
    }

    fun sendInternalBannerGroup(bannerGroupViewModel: BannerGroupViewModel){

        if(!bannerGroupViewModel.userId.isNullOrEmpty() && !bannerGroupViewModel.sessionId.isNullOrEmpty()){
            ConnectionManager.getEventFactory().sendInternalBannerGroup(bannerGroupViewModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
        else{
            SegmentifyLogger.printErrorLog("You must fill userid&sessionid before accessing internal banner group event")
            return
        }
    }

    private fun reformatResponse(eventResponseModel: EventResponseModel) : ArrayList<RecommendationModel> {
        return RecommendationResponseParser.parse(eventResponseModel) { instanceId, actionId ->
            val interactionModel = InteractionModel()
            interactionModel.eventName = Constant.interactionEventName
            interactionModel.type = "impression"
            interactionModel.instanceId = instanceId
            interactionModel.interactionId = actionId
            sendInteractionEvent(interactionModel)
        }
    }
}
