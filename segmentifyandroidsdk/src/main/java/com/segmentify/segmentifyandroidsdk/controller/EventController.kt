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
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger

internal object EventController {

    /**
     * Runs [send] once the event carries a user id and a session id. Events
     * created before the ids arrived (first launch) wait for them instead of
     * being dropped; ids set on the model by the caller are never overwritten.
     */
    private fun whenSessionReady(event: SegmentifyObject, onError: ((SegmentifyException) -> Unit)? = null, send: () -> Unit) {
        if (SegmentifyManager.clientPreferences == null) {
            report(SegmentifyException(ConnectionManager.NOT_CONFIGURED), onError)
            return
        }
        val sendOrReport = {
            try {
                send()
            } catch (e: SegmentifyException) {
                report(e, onError)
            }
        }
        if (!event.userId.isNullOrEmpty() && !event.sessionId.isNullOrEmpty()) {
            sendOrReport()
            return
        }
        KeyController.pendingEvents.submit({ ids ->
            if (event.userId.isNullOrEmpty()) {
                event.userId = ids.userId
            }
            if (event.sessionId.isNullOrEmpty()) {
                event.sessionId = ids.sessionId
            }
            sendOrReport()
        }, { cause ->
            if (onError != null) {
                onError(givenUp(cause))
            }
        })
    }

    /** An event that could not be sent: logged, and handed to the host when it gave a callback. */
    private fun report(error: SegmentifyException, onError: ((SegmentifyException) -> Unit)?) {
        SegmentifyLogger.printErrorLog(error.message.toString())
        onError?.invoke(error)
    }

    /** The error for an event that never left the queue, carrying the reason when one is known. */
    private fun givenUp(cause: Throwable?): SegmentifyException {
        if (cause == null) {
            return SegmentifyException("The event was given up: too many events were waiting for a user id and session id")
        }
        val status = (cause as? SegmentifyException)?.httpStatus
        return SegmentifyException("The event was given up, no user id and session id: " + cause.message, status, cause)
    }

    fun sendPageView(pageModel : PageModel,segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>){

        whenSessionReady(pageModel, { segmentifyCallback.onError(it) }) {

            ConnectionManager.getEventFactory().sendPageView(pageModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>(){
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            segmentifyCallback.onError(error)
                        }
                    })
        }

    }

    fun sendSearchView(pageModel: SearchPageModel, segmentifyCallback: SegmentifyCallback<SearchResponseModel>){
        whenSessionReady(pageModel, { segmentifyCallback.onError(it) }) {
            ConnectionManager.getEventFactory().sendSearchView(pageModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<SearchEventResponseModel>(){
                        override fun onSuccess(response: SearchEventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatSearchResponse(response))
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            segmentifyCallback.onError(error)
                        }
                    })
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
        whenSessionReady(pageModel, { segmentifyCallback.onError(it) }) {
            ConnectionManager.getEventFactory()
                .sendFacetedSearchView(pageModel, SegmentifyManager.configModel.apiKey!!)
                .enqueue(object : NetworkCallback<SearchFacetedEventResponseModel>() {
                    override fun onSuccess(response: SearchFacetedEventResponseModel) {
                        segmentifyCallback.onDataLoaded(reformatSearchFacetedResponse(response))
                    }

                    override fun onGiveUp(error: SegmentifyException) {
                        segmentifyCallback.onError(error)
                    }
                })
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

        whenSessionReady(customEventModel, { segmentifyCallback.onError(it) }) {


            ConnectionManager.getEventFactory().sendCustomEvent(customEventModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>() {
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            segmentifyCallback.onError(error)
                        }
                    })
        }


    }

    fun sendProductView(productModel: ProductModel, segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>){


        whenSessionReady(productModel, { segmentifyCallback.onError(it) }) {


            ConnectionManager.getEventFactory().sendProductView(productModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>() {
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            segmentifyCallback.onError(error)
                        }
                    })
        }
    }

    fun sendAddOrRemoveBasket(basketModel: BasketModel){

        whenSessionReady(basketModel) {

            ConnectionManager.getEventFactory().sendAddOrRemoveBasket(basketModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }

    }

    fun sendCheckout(checkoutModel: CheckoutModel,segmentifyCallback: SegmentifyCallback<ArrayList<RecommendationModel>>) {

        whenSessionReady(checkoutModel, { segmentifyCallback.onError(it) }) {
            ConnectionManager.getEventFactory().sendPurchase(checkoutModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<EventResponseModel>(){
                        override fun onSuccess(response: EventResponseModel) {
                            segmentifyCallback.onDataLoaded(reformatResponse(response))
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            segmentifyCallback.onError(error)
                        }
                    })
        }

    }

    fun sendUserOperation(userModel: UserModel){

        whenSessionReady(userModel) {

            ConnectionManager.getEventFactory().sendUserOperation(userModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
    }

    fun sendUserTraits(userTraitsEventModel: UserTraitsEventModel) {

        whenSessionReady(userTraitsEventModel) {

            ConnectionManager.getEventFactory().sendUserTraits(userTraitsEventModel, SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>() {
                        override fun onSuccess(response: Any) {
                        }
                    })
        }
    }

    fun sendCdpUserEvent(model: UserTraitsEventModel, identityPayload: CdpEventsPayload? = null) {

        whenSessionReady(model) {

            ConnectionManager.getEventFactory().sendUserTraits(model, SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>() {
                        override fun onSuccess(response: Any) {
                            if (identityPayload != null) {
                                CdpUserProfileStore.default.markIdentifySent(identityPayload)
                            }
                        }

                        override fun onEmptySuccess(error: SegmentifyException) {
                            if (identityPayload != null) {
                                CdpUserProfileStore.default.markIdentifySent(identityPayload)
                            }
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            if (identityPayload != null) {
                                CdpUserProfileStore.default.clearInFlightIdentify(identityPayload)
                            }
                        }
                    })
        }
    }

    fun sendChangeUser(userChangeModel: UserChangeModel,segmentifyCallback : SegmentifyCallback<Boolean>){

        whenSessionReady(userChangeModel, { segmentifyCallback.onError(it) }) {

            ConnectionManager.getEventFactory().sendChangeUser(userChangeModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                            segmentifyCallback.onDataLoaded(true)
                        }

                        override fun onEmptySuccess(error: SegmentifyException) {
                            segmentifyCallback.onDataLoaded(true)
                        }

                        override fun onGiveUp(error: SegmentifyException) {
                            segmentifyCallback.onError(error)
                        }
                    })
        }


    }

    fun sendInteractionEvent(interactionModel: InteractionModel){

        whenSessionReady(interactionModel) {
            ConnectionManager.getEventFactory().sendInteractionEvent(interactionModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }

    }

    fun sendBannerOperations(bannerOperationOperationsModel: BannerOperationsModel){

        whenSessionReady(bannerOperationOperationsModel) {
            ConnectionManager.getEventFactory().sendBannerOperations(bannerOperationOperationsModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
        }

    }

    fun sendBannerGroupView(bannerGroupViewModel: BannerGroupViewModel){

        whenSessionReady(bannerGroupViewModel) {
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
    }

    fun sendInternalBannerGroup(bannerGroupViewModel: BannerGroupViewModel){

        whenSessionReady(bannerGroupViewModel) {
            ConnectionManager.getEventFactory().sendInternalBannerGroup(bannerGroupViewModel,SegmentifyManager.configModel.apiKey!!)
                    .enqueue(object : NetworkCallback<Any>(){
                        override fun onSuccess(response: Any) {
                        }
                    })
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
