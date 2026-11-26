package com.segmentify.segmentifyandroidsdk

import com.segmentify.segmentifyandroidsdk.model.EventResponseModel
import com.segmentify.segmentifyandroidsdk.network.NetworkCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        val errors = mutableListOf<SegmentifyException>()

        override fun onGiveUp(error: SegmentifyException) {
            errors.add(error)
            super.onGiveUp(error)
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

    // SFY-28788: the reason travels with the give-up.

    @Test
    fun httpErrorIsReportedWithItsStatus() {
        val callback = RecordingCallback()

        callback.onResponse(FakeCall(), serverError())

        assertEquals(listOf<Int?>(500), callback.errors.map { it.httpStatus })
        assertEquals(1, callback.giveUps)
    }

    @Test
    fun transportFailureIsReportedWithItsCause() {
        val callback = RecordingCallback()
        val cause = IOException("offline")

        callback.onFailure(FakeCall(), cause)

        assertEquals(1, callback.errors.size)
        assertEquals(null, callback.errors[0].httpStatus)
        assertEquals(cause, callback.errors[0].cause)
    }

    @Test
    fun emptySuccessBodyIsReportedAsAnError() {
        val callback = RecordingCallback()

        callback.onResponse(FakeCall(), Response.success<String>(null))

        assertEquals(1, callback.errors.size)
        assertEquals(0, callback.successes)
    }

    @Test
    fun emptySuccessBodyCanBeAcceptedByTheCaller() {
        var accepted = 0
        val callback = object : NetworkCallback<String>() {
            var giveUps = 0

            override fun onSuccess(response: String) {}

            override fun onEmptySuccess(error: SegmentifyException) {
                accepted++
            }

            override fun onGiveUp() {
                giveUps++
            }
        }

        callback.onResponse(FakeCall(), Response.success<String>(null))

        assertEquals(1, accepted)
        assertEquals(0, callback.giveUps)
    }

    // The backend rejects events with HTTP 200 and a statusCode other than SUCCESS.

    private class AnyCallback<T> : NetworkCallback<T>() {
        var successes = 0
        val errors = mutableListOf<SegmentifyException>()

        override fun onSuccess(response: T) {
            successes++
        }

        override fun onGiveUp(error: SegmentifyException) {
            errors.add(error)
        }
    }

    @Test
    fun rejectedEventInAnUntypedBodyIsAnError() {
        val callback = AnyCallback<Any>()

        callback.onResponse(null, Response.success<Any>(mapOf("statusCode" to "NO_SESSIONID", "timestamp" to 1.0)))

        assertEquals(0, callback.successes)
        assertEquals(listOf<String?>("NO_SESSIONID"), callback.errors.map { it.statusCode })
        assertEquals(listOf<Int?>(200), callback.errors.map { it.httpStatus })
    }

    @Test
    fun rejectedEventInATypedBodyIsAnError() {
        val callback = AnyCallback<EventResponseModel>()
        val body = EventResponseModel()
        body.statusCode = "BOT"

        callback.onResponse(null, Response.success(body))

        assertEquals(0, callback.successes)
        assertEquals(listOf<String?>("BOT"), callback.errors.map { it.statusCode })
    }

    @Test
    fun successStatusCodeAndMissingStatusCodeAreSuccesses() {
        val untyped = AnyCallback<Any>()
        untyped.onResponse(null, Response.success<Any>(mapOf("statusCode" to "SUCCESS")))
        untyped.onResponse(null, Response.success<Any>(mapOf("responses" to emptyList<Any>())))

        val typed = AnyCallback<EventResponseModel>()
        typed.onResponse(null, Response.success(EventResponseModel()))

        assertEquals(2, untyped.successes)
        assertEquals(1, typed.successes)
        assertTrue(untyped.errors.isEmpty() && typed.errors.isEmpty())
    }
}
