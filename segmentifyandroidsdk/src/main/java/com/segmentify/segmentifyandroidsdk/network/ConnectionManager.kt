package com.segmentify.segmentifyandroidsdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.network.Factories.EventFactory
import com.segmentify.segmentifyandroidsdk.network.Factories.PushFactory
import com.segmentify.segmentifyandroidsdk.network.Factories.UserSessionFactory
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyUserAgent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ConnectionManager {
    private val timeoutInterval = 60
    private var userSessionFactory: UserSessionFactory
    private var eventFactory: EventFactory
    private lateinit var pushFactory: PushFactory
    private val client: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor()

        if (SegmentifyManager.clientPreferences?.isLogVisible()!!) {
            logging.level = HttpLoggingInterceptor.Level.BODY
        } else {
            logging.level = HttpLoggingInterceptor.Level.NONE
        }

        val httpClient = OkHttpClient.Builder()

        //Gelen response kontrol edilecek
        httpClient.addInterceptor(logging).addInterceptor(Interceptor { chain ->
            val request = chain?.request()
            val newRequest: Request

            try {
                newRequest = request?.newBuilder()
                        ?.addHeader("Origin", SegmentifyManager.configModel.subDomain)
                        ?.addHeader("Content-Type", "application/json")
                        ?.addHeader("Accept", "application/json")
                        ?.header(SegmentifyUserAgent.HEADER_NAME, SegmentifyUserAgent.current)!!.build()
            } catch (e: Exception) {
                Log.d("addHeader", "Error")
                e.printStackTrace()
                return@Interceptor chain?.proceed(request)!!
            }

            /*if(SegmentifyManager.clientPreferences != null && SegmentifyManager.clientPreferences!!.getSessionId().isNullOrBlank()){
                val getSessionIdRequest = Request.Builder().header("Content-Type", "application/json").header("Accept", "application/json").get().url(BuildConfig.KEY_ADDRESS + "get/key?count=1").build()
                var response = getSyncClient().newCall(getSessionIdRequest).execute()
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                var sessionIdResponse = Gson().fromJson<ArrayList<String>>(response.body().toString(), listType)

                SegmentifyManager.clientPreferences?.setSessionId(sessionIdResponse[0])
            }

            if(SegmentifyManager.clientPreferences != null && SegmentifyManager.clientPreferences!!.getUserId().isNullOrBlank()){
                val getUserIDSessionIdRequest = Request.Builder().header("Content-Type", "application/json").header("Accept", "application/json").get().url(BuildConfig.KEY_ADDRESS + "get/key?count=2").build()
                var response = getSyncClient().newCall(getUserIDSessionIdRequest).execute()
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                var userIdSessionIdResponse = Gson().fromJson<ArrayList<String>>(response.body().toString(), listType)

                SegmentifyManager.clientPreferences?.setUserId(userIdSessionIdResponse[0])
                SegmentifyManager.clientPreferences?.setSessionId(userIdSessionIdResponse[1])
            }*/

            chain.proceed(newRequest)
        })
        httpClient.connectTimeout(timeoutInterval.toLong(), TimeUnit.SECONDS)
        httpClient.readTimeout(timeoutInterval.toLong(), TimeUnit.SECONDS)

        client = httpClient.build()
        val keyService = Retrofit.Builder()
                .baseUrl(SegmentifyManager.clientPreferences?.getApiUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        userSessionFactory = keyService.create(UserSessionFactory::class.java)

        val eventService = Retrofit.Builder()
                .baseUrl(SegmentifyManager.clientPreferences?.getApiUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        eventFactory = eventService.create(EventFactory::class.java)

        if (SegmentifyManager.configModel?.dataCenterUrlPush != null) {
            val pushService = Retrofit.Builder()
                    .baseUrl(SegmentifyManager.configModel?.dataCenterUrlPush)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

            pushFactory = pushService.create(PushFactory::class.java)
        }
    }

    fun getUserSessionFactory(): UserSessionFactory {
        return userSessionFactory
    }

    fun getEventFactory(): EventFactory {
        return eventFactory
    }

    fun getPushFactory(): PushFactory {
        return pushFactory
    }

    fun rebuildServices() {
        val eventService = Retrofit.Builder()
                .baseUrl(SegmentifyManager.clientPreferences?.getApiUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        eventFactory = eventService.create(EventFactory::class.java)

        if (SegmentifyManager.configModel?.dataCenterUrlPush != null) {
            val pushService = Retrofit.Builder()
                    .baseUrl(SegmentifyManager.configModel?.dataCenterUrlPush)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            pushFactory = pushService.create(PushFactory::class.java)
        }
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