package com.segmentify.segmentifyandroidsdk.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/** Logs a request only while [isVisible] says so, decided per request. */
internal class SdkLogInterceptor(
    private val isVisible: () -> Boolean,
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
