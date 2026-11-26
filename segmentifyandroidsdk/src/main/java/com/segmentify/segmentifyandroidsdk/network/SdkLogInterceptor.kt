package com.segmentify.segmentifyandroidsdk.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Logs a request only while [isVisible] says so. It is asked for every
 * request, so SegmentifyManager.logStatus takes effect at once instead of
 * only before the first request.
 */
internal class SdkLogInterceptor(
    private val isVisible: () -> Boolean,
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
) : Interceptor {

    private val logging = HttpLoggingInterceptor(logger).setLevel(HttpLoggingInterceptor.Level.BODY)

    override fun intercept(chain: Interceptor.Chain): Response {
        return if (isVisible()) logging.intercept(chain) else chain.proceed(chain.request())
    }
}
