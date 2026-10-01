package com.segmentify.segmentifyandroidsdk.utils

class Constant {
    companion object {

        //Keys
        val tokenKey = "SEGMENTIFY_TOKEN_KEY"
        val registerKey = "SEGMENTIFY_REGISTER_KEY"
        val lastRequestDateKey = "SEGMENTIFY_LAST_REQUEST_DATE_KEY"


        //Events
        val pageViewEventName = "PAGE_VIEW"
        val productViewEventName = "PRODUCT_VIEW"
        val basketOperationsEventName = "BASKET_OPERATIONS"
        val checkoutEventName = "CHECKOUT"
        val userOperationEventName = "USER_OPERATIONS"
        val customEventName = "CUSTOM_EVENT"
        val interactionEventName = "INTERACTION"
        val bannerOperationsEventName = "BANNER_OPERATIONS"
        val bannerGroupViewEventName = "BANNER_GROUP_VIEW"
        val internalBannerGroupEventName = "INTERNAL_BANNER_GROUP"
        val searchViewEventName = "SEARCH"
        val userTraitsEventName = "USER_TRAITS"
        val userIdentityEventName = "USER_IDENTITY"
        val userRegisterEventName = "USER_REGISTER"
        val userLoginEventName = "USER_LOGIN"
        val userLogoutEventName = "USER_LOGOUT"
        val userSubscribeEventName = "USER_SUBSCRIBE"
        val userUnsubscribeEventName = "USER_UNSUBSCRIBE"

        //Steps
        val customerInformationStep = "customer"
        val viewBasketStep = "view-basket"
        val paymentInformationStep = "payment-info"
        val paymentPurchaseStep = "purchase"
        val signInStep = "signin"
        val registerStep = "signup"
        val logoutStep = "signout"
        val updateUserStep = "update"
        val deleteLastSearchStep = "delete_last_search"
        val impressionStep = "impression"
        val widgetViewStep = "widget-view"
        val clickStep = "click"
        val bannerImpressionStep = "impression"
        val bannerClickStep = "click"
        val bannerUpdateStep = "update"
        val searchStep = "search"
        val pushStep = "push"
        val pushClickStep = "push-click"

        //Custom
        val sessionKeepSecond = 86400
        val segmentfyErrorLog = "Segmentify Error"
        val segmentfySuccessLog = "Segmentify Success"

    }
}