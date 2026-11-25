package com.segmentify.segmentifyandroidsdk

import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/** Every request must end in exactly one onSuccess or onGiveUp. */
class NetworkCallbackTest {

    private class RecordingCallback : NetworkCallback<String>() {
        var successes = 0
        var giveUps = 0

        override fun onSuccess(response: String) {
            successes++
        }

        override fun onGiveUp() {
            giveUps++
        }
    }

    /** A call whose retries answer with [answer] straight away. */
    private class FakeCall(
        private val canceled: Boolean = false,
        private val answer: () -> Response<String> = { serverError() }
    ) : Call<String> {
        var enqueued = 0

        override fun enqueue(callback: Callback<String>) {
            enqueued++
            callback.onResponse(this, answer())
        }

        override fun clone(): Call<String> = this
        override fun isCanceled(): Boolean = canceled
        override fun execute(): Response<String> = answer()
        override fun isExecuted(): Boolean = enqueued > 0
        override fun cancel() {}
        override fun request(): Request = Request.Builder().url("https://segmentify-sdk.test/").build()
        override fun timeout(): Timeout = Timeout.NONE
    }

    private companion object {
        fun serverError(): Response<String> = Response.error(500, ResponseBody.create(null, ""))
    }

    @Test
    fun successWithBodyCallsOnSuccess() {
        val callback = RecordingCallback()

        callback.onResponse(FakeCall(), Response.success("ok"))

        assertEquals(1, callback.successes)
        assertEquals(0, callback.giveUps)
    }

    @Test
    fun errorResponseIsRetriedThreeTimesThenGivesUpOnce() {
        val callback = RecordingCallback()
        val call = FakeCall()

        callback.onResponse(call, serverError())

        assertEquals(3, call.enqueued)
        assertEquals(1, callback.giveUps)
        assertEquals(0, callback.successes)
    }

    @Test
    fun transportFailureGivesUpOnce() {
        val callback = RecordingCallback()

        callback.onFailure(FakeCall(), IOException("offline"))

        assertEquals(1, callback.giveUps)
    }

    // Code review follow-ups: exits that used to report nothing.

    @Test
    fun errorResponseOnACanceledCallGivesUp() {
        val callback = RecordingCallback()
        val call = FakeCall(canceled = true)

        callback.onResponse(call, serverError())

        assertEquals(0, call.enqueued)
        assertEquals(1, callback.giveUps)
    }

    @Test
    fun missingResponseGivesUp() {
        val callback = RecordingCallback()

        callback.onResponse(FakeCall(), null)

        assertEquals(1, callback.giveUps)
    }

    @Test
    fun errorResponseWithoutACallGivesUp() {
        val callback = RecordingCallback()

        callback.onResponse(null, serverError())

        assertEquals(1, callback.giveUps)
    }
}
