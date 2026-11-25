package com.segmentify.segmentifyandroidsdk.network

import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class NetworkCallback<T> : Callback<T> {

    private val TOTAL_RETRIES = 3
    private var retryCount = 0

    override fun onResponse(call: Call<T>?, response: Response<T>?) {
        response?.apply {
            if(isSuccessful)
            {
                val body = response.body()
                if(body != null){
                    onSuccess(body)
                } else {
                    SegmentifyLogger.printErrorLog("Request successful but body is null")
                    onGiveUp()
                }
            }
            else if(response.code() != 200){
                if(call != null && !call.isCanceled) run {
                    SegmentifyLogger.printErrorLog(response.message())
                    if (retryCount++ < TOTAL_RETRIES) {
                        SegmentifyLogger.printErrorLog(": Retrying... ($retryCount out of $TOTAL_RETRIES)")
                        retry(call)
                    } else {
                        onGiveUp()
                    }
                }
            }
        }
    }

    abstract fun onSuccess(response:T)

    /** Called once when the request ends without a usable response and will not be retried. */
    open fun onGiveUp() {
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        SegmentifyLogger.printErrorLog("Request failed: " + t.message)
        onGiveUp()
    }

    private fun retry(call: Call<T>) {
        call.clone().enqueue(this)
    }

}