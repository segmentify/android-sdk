package com.segmentify.segmentifyandroidsdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.segmentify.segmentifyandroidsdk.SegmentifyManager
import com.segmentify.segmentifyandroidsdk.network.Factories.EventFactory
import com.segmentify.segmentifyandroidsdk.network.Factories.PushFactory
import com.segmentify.segmentifyandroidsdk.network.Factories.UserSessionFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit


object ConnectionManager {
    private val timeoutInterval = 60
    private var userSessionFactory: UserSessionFactory
    private var eventFactory: EventFactory
    private lateinit var pushFactory: PushFactory
    private val client: OkHttpClient
    private val pushClient: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor()

        if (SegmentifyManager.clientPreferences != null && SegmentifyManager.clientPreferences!!.isLogVisible()) {
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
                val authToken = SegmentifyManager.clientPreferences?.getAuthToken()
                val apiKey = SegmentifyManager.configModel.apiKey

                val requestBuilder = request?.newBuilder()
                        ?.addHeader("Origin", SegmentifyManager.configModel.subDomain ?: "")
                        ?.addHeader("Content-Type", "application/json")
                        ?.addHeader("Accept", "application/json")

                if (!authToken.isNullOrBlank()) {
                    requestBuilder?.addHeader("Authorization", "Basic $authToken")
                } else if (!apiKey.isNullOrBlank()) {
                    requestBuilder?.addHeader("Authorization", "Basic $apiKey")
                } else {
                    Log.e("ConnectionManager", "No authToken or apiKey provided. Requests will not be authenticated.")
                    throw IllegalStateException("No authToken or apiKey provided. Please configure authentication via setConfig or setAuthToken.")
                }

                newRequest = requestBuilder!!.build()
            } catch (e: Exception) {
                Log.d("addHeader", "Error")
                e.printStackTrace()
                return@Interceptor chain?.proceed(request!!)!!
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

        val pushHttpClient = OkHttpClient.Builder()
        pushHttpClient.addInterceptor(logging).addInterceptor(Interceptor { chain ->
            val request = chain.request()
            val newRequest: Request
            try {
                val authToken = SegmentifyManager.clientPreferences?.getAuthToken()
                val apiKey = SegmentifyManager.configModel.apiKey

                val requestBuilder = request.newBuilder()
                        .addHeader("Origin", SegmentifyManager.configModel.subDomain ?: "")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")

                if (!authToken.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Basic $authToken")
                } else if (!apiKey.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Basic $apiKey")
                } else {
                    Log.e("ConnectionManager", "No authToken or apiKey provided. Requests will not be authenticated.")
                    throw IllegalStateException("No authToken or apiKey provided. Please configure authentication via setConfig or setAuthToken.")
                }

                newRequest = requestBuilder.build()
            } catch (e: Exception) {
                Log.d("addHeader", "Error")
                e.printStackTrace()
                return@Interceptor chain.proceed(request)
            }

            // Log the full request
            val buffer = okio.Buffer()
            newRequest.body()?.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            Log.d("PushRequest", "=== FULL PUSH REQUEST ===")
            Log.d("PushRequest", "URL: ${newRequest.url()}")
            Log.d("PushRequest", "Method: ${newRequest.method()}")
            Log.d("PushRequest", "Headers: ${newRequest.headers()}")
            Log.d("PushRequest", "Body: $bodyString")
            Log.d("PushRequest", "=========================")

            chain.proceed(newRequest)
        })
        pushHttpClient.connectTimeout(timeoutInterval.toLong(), TimeUnit.SECONDS)
        pushHttpClient.readTimeout(timeoutInterval.toLong(), TimeUnit.SECONDS)
        pushClient = pushHttpClient.build()

        val apiUrl = SegmentifyManager.clientPreferences?.getApiUrl()
        if (!apiUrl.isNullOrBlank()) {
            val keyService = Retrofit.Builder()
                    .baseUrl(apiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

            userSessionFactory = keyService.create(UserSessionFactory::class.java)

            val eventService = Retrofit.Builder()
                    .baseUrl(apiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

            eventFactory = eventService.create(EventFactory::class.java)
        } else {
            // Provide a dummy factory if not initialized yet, will be rebuilt in rebuildServices
            val dummyRetrofit = Retrofit.Builder()
                    .baseUrl("http://localhost/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            userSessionFactory = dummyRetrofit.create(UserSessionFactory::class.java)
            eventFactory = dummyRetrofit.create(EventFactory::class.java)
        }

        if (SegmentifyManager.configModel.dataCenterUrlPush != null) {
            val pushService = Retrofit.Builder()
                    .baseUrl(SegmentifyManager.configModel.dataCenterUrlPush ?: "")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(pushClient)
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
        val apiUrl = SegmentifyManager.clientPreferences?.getApiUrl()
        if (!apiUrl.isNullOrBlank()) {
            val eventService = Retrofit.Builder()
                    .baseUrl(apiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            eventFactory = eventService.create(EventFactory::class.java)

            val keyService = Retrofit.Builder()
                    .baseUrl(apiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            userSessionFactory = keyService.create(UserSessionFactory::class.java)
        }

        if (SegmentifyManager.configModel.dataCenterUrlPush != null) {
            val pushService = Retrofit.Builder()
                    .baseUrl(SegmentifyManager.configModel.dataCenterUrlPush ?: "")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(pushClient)
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