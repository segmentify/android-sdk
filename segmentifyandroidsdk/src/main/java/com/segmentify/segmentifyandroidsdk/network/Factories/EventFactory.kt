package com.segmentify.segmentifyandroidsdk.network.Factories

import com.segmentify.segmentifyandroidsdk.model.*
import com.segmentify.segmentifyandroidsdk.model.faceted.SearchFacetedEventResponseModel
import com.segmentify.segmentifyandroidsdk.model.faceted.SearchFacetedPageModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface EventFactory {

        //POST PageModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendPageView(@Body pageModel: PageModel): Call<EventResponseModel>

        //POST CheckoutModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendPurchase(@Body checkoutModel: CheckoutModel): Call<EventResponseModel>

        //POST CustomEventModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendCustomEvent(@Body customEventModel: CustomEventModel): Call<EventResponseModel>

        //POST BasketModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendAddOrRemoveBasket(@Body basketModel: BasketModel): Call<Any>

        //POST ProductModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendProductView(@Body productModel: ProductModel): Call<EventResponseModel>

        //POST UserModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendUserOperation(@Body userModel: UserModel): Call<Any>

        @POST("/add/events/v1.json")
        fun sendUserTraits(@Body userTraitsEventModel: UserTraitsEventModel): Call<Any>

        //POST UserModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendChangeUser(@Body userChangeModel: UserChangeModel): Call<Any>

        //POST Interaction Object with types as defined in documentation
        @POST("/add/events/v1.json")
        fun sendInteractionEvent(@Body interactionModel: InteractionModel): Call<Any>

        //POST Banner Operation Object with types as defined in documentation
        @POST("/add/events/v1.json")
        fun sendBannerOperations(@Body bannerOperationsModel: BannerOperationsModel): Call<Any>

        //POST Banner Group View Object with types as defined in documentation
        @POST("/add/events/v1.json")
        fun sendBannerGroupView(@Body bannerGroupViewModel: BannerGroupViewModel): Call<Any>

        //POST Banner Internal Group is an internal event
        @POST("/add/events/v1.json")
        fun sendInternalBannerGroup(@Body bannerGroupViewModel: BannerGroupViewModel): Call<Any>

        //POST PageModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendSearchView(@Body pageModel: SearchPageModel): Call<SearchEventResponseModel>

        //POST PageModel Object with steps as defined in documentation
        @POST("/add/events/v1.json")
        fun sendFacetedSearchView(@Body pageModel: SearchFacetedPageModel): Call<SearchFacetedEventResponseModel>

}
