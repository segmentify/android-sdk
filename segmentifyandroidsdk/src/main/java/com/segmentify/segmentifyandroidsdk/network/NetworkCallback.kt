package com.segmentify.segmentifyandroidsdk.network

import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class NetworkCallback<T> : Callback<T> {

    private val TOTAL_RETRIES = 3
    private var retryCount = 0

    override fun onResponse(call: Call<T>?, response: Response<T>?) {
        if (response == null) {
            giveUp(SegmentifyException("Request ended without a response"))
            return
        }

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                onSuccess(body)
            } else {
                onEmptySuccess(SegmentifyException("Request successful but body is null", response.code()))
            }
            return
        }

        SegmentifyLogger.printErrorLog(response.message())
        if (call != null && !call.isCanceled && retryCount++ < TOTAL_RETRIES) {
            SegmentifyLogger.printErrorLog(": Retrying... ($retryCount out of $TOTAL_RETRIES)")
            retry(call)
        } else {
            giveUp(SegmentifyException("Server answered with HTTP " + response.code(), response.code()))
        }
    }

    abstract fun onSuccess(response:T)

    /** Called once when the request ends without a usable response and will not be retried. */
    open fun onGiveUp() {
    }

    /**
     * A 2xx answer without a body. A give-up by default; override where the
     * request needs no body to count as done.
     */
    open fun onEmptySuccess(error: SegmentifyException) {
        giveUp(error)
    }

    /** Same moment as [onGiveUp], with the reason. Calls [onGiveUp] by default. */
    open fun onGiveUp(error: SegmentifyException) {
        onGiveUp()
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        giveUp(SegmentifyException("Request failed: " + t.message, null, t))
    }

    private fun giveUp(error: SegmentifyException) {
        SegmentifyLogger.printErrorLog(error.message.toString())
        onGiveUp(error)
    }

    private fun retry(call: Call<T>) {
        call.clone().enqueue(this)
    }

}