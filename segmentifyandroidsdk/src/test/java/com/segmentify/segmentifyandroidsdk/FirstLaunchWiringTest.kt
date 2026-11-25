package com.segmentify.segmentifyandroidsdk

import android.os.Looper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.segmentify.segmentifyandroidsdk.model.PageModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * SFY-28787, end to end: the public API, the real controllers and Retrofit,
 * with only the backend replaced by a local server.
 *
 * The SDK is built from process-wide singletons that are configured once, so
 * this class holds a single scenario.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstLaunchWiringTest {

    private val server = MockWebServer()
    private val keyRequests = CopyOnWriteArrayList<String>()
    private val events = CopyOnWriteArrayList<JsonObject>()

    @Before
    fun startServer() {
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                if (path.startsWith("/get/key")) {
                    keyRequests.add(path)
                    val number = keyRequests.size
                    val keys = if (path.contains("count=2")) {
                        "[\"user-$number\",\"session-$number\"]"
                    } else {
                        "[\"session-$number\"]"
                    }
                    return MockResponse().setBody(keys)
                }
                events.add(JsonParser.parseString(request.body.readUtf8()).asJsonObject)
                return MockResponse().setBody("{\"responses\":[[]]}")
            }
        })
        server.start()
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    /** Retrofit delivers callbacks on the main looper, which Robolectric only runs on request. */
    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue("timed out waiting for $what", condition())
    }

    /** Gives requests that should not happen a chance to arrive. */
    private fun settle() {
        val until = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < until) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }

    @Test
    fun pageViewSentRightAfterConfigOnFirstLaunchWaitsForTheIdsAndIsSentOnce() {
        var callbacks = 0
        val dataCenterUrl = server.url("/").toString().removeSuffix("/")

        SegmentifyManager.config(RuntimeEnvironment.getApplication(), "test-api-key", dataCenterUrl, "segmentify-sdk.test")
        val page = PageModel()
        page.category = "Home Page"
        SegmentifyManager.sendPageView(page, object : SegmentifyCallback<ArrayList<RecommendationModel>> {
            override fun onDataLoaded(data: ArrayList<RecommendationModel>) {
                callbacks++
            }
        })

        await("the page view callback") { callbacks == 1 }
        settle()

        assertEquals("one key request on first launch", listOf("/get/key?count=2"), keyRequests.toList())
        assertEquals(1, events.size)
        assertEquals("PAGE_VIEW", events[0].get("name").asString)
        assertEquals("Home Page", events[0].get("category").asString)
        assertEquals("user-1", events[0].get("userId").asString)
        assertEquals("session-1", events[0].get("sessionId").asString)
        assertEquals(1, callbacks)
    }
}
