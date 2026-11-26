package com.segmentify.segmentifyandroidsdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.network.Factories.EventFactory
import com.segmentify.segmentifyandroidsdk.network.Factories.PushFactory
import com.segmentify.segmentifyandroidsdk.network.Factories.UserSessionFactory
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyUserAgent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * The SDK's HTTP client and Retrofit services.
 *
 * Nothing here reads the configuration when the object is created, so it can
 * be touched before SegmentifyManager.config. Each service is looked up for
 * the URL configured at the moment of the call: a service used before its URL
 * is configured fails with a [SegmentifyException], and a changed URL gets a
 * new service.
 */
object ConnectionManager {

    internal const val NOT_CONFIGURED = "SegmentifyManager.config must be called before sending events"
    internal const val PUSH_NOT_CONFIGURED = "SegmentifyManager.setPushConfig must be called before sending push events"

    private const val TIMEOUT_SECONDS = 60L

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // The SDK's headers first, so that the log shows the request as it is sent.
            .addInterceptor(Interceptor { chain -> withSdkHeaders(chain) })
            .addInterceptor(SdkLogInterceptor({ SegmentifyManager.clientPreferences?.isLogVisible() == true }))
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit services by type and base URL. */
    private val services = ConcurrentHashMap<String, Any>()

    private fun withSdkHeaders(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val withHeaders = try {
            val builder = request.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .header(SegmentifyUserAgent.HEADER_NAME, SegmentifyUserAgent.current)
            SegmentifyManager.configModel.subDomain?.let { builder.addHeader("Origin", it) }
            builder.build()
        } catch (e: Exception) {
            Log.d("addHeader", "Error", e)
            request
        }
        return chain.proceed(withHeaders)
    }

    private fun <T> service(baseUrl: String?, type: Class<T>, notConfigured: String): T {
        if (baseUrl.isNullOrBlank()) {
            throw SegmentifyException(notConfigured)
        }
        val service = services.getOrPut(type.name + " " + baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(type)
        }
        return type.cast(service)!!
    }

    fun getUserSessionFactory(): UserSessionFactory {
        return service(SegmentifyManager.clientPreferences?.getApiUrl(), UserSessionFactory::class.java, NOT_CONFIGURED)
    }

    fun getEventFactory(): EventFactory {
        return service(SegmentifyManager.clientPreferences?.getApiUrl(), EventFactory::class.java, NOT_CONFIGURED)
    }

    fun getPushFactory(): PushFactory {
        return service(SegmentifyManager.configModel.dataCenterUrlPush, PushFactory::class.java, PUSH_NOT_CONFIGURED)
    }

    /**
     * Services follow the configured URLs on their own; this only drops the
     * ones built so far. Kept because it is public.
     */
    fun rebuildServices() {
        services.clear()
    }

    fun getSyncClient(): OkHttpClient {
        return client
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connectivityManager.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }
}
