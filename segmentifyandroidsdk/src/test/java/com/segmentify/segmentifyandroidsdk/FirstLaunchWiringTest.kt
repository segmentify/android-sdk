package com.segmentify.segmentifyandroidsdk

import okhttp3.logging.HttpLoggingInterceptor
import com.segmentify.segmentifyandroidsdk.network.ConnectionManager
import com.segmentify.segmentifyandroidsdk.controller.SessionWindow
import android.os.Looper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.segmentify.segmentifyandroidsdk.model.PageModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyUserAgent
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * End to end: the public API, the real controllers and Retrofit, with only the
 * backend replaced by a local server.
 *
 * The SDK is built from process-wide singletons and its HTTP client keeps the
 * first base URL it was configured with, so every scenario here shares one
 * server. Robolectric gives each test a fresh install (empty preferences), so
 * every scenario starts as a first launch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstLaunchWiringTest {

    companion object {
        private val server = MockWebServer()
        private val keyRequests = CopyOnWriteArrayList<String>()
        private val events = CopyOnWriteArrayList<JsonObject>()
        private val userAgents = CopyOnWriteArrayList<String?>()

        /** Status the server answers events with. */
        @Volatile
        private var eventStatus = 200

        /** Body the server answers events with. */
        @Volatile
        private var eventBody = "{\"statusCode\":\"SUCCESS\",\"responses\":[[]]}"

        /** Status the server answers key requests with. */
        @Volatile
        private var keyStatus = 200

        @BeforeClass
        @JvmStatic
        fun startServer() {
            server.setDispatcher(object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path ?: ""
                    userAgents.add(request.getHeader("User-Agent"))
                    if (path.startsWith("/get/key")) {
                        keyRequests.add(path)
                        // A new session id for every key request: session-1, session-2, ...
                        val session = "session-${keyRequests.size}"
                        val keys = if (path.contains("count=2")) "[\"user-1\",\"$session\"]" else "[\"$session\"]"
                        return MockResponse().setResponseCode(keyStatus).setBody(keys)
                    }
                    events.add(JsonParser.parseString(request.body.readUtf8()).asJsonObject)
                    return MockResponse().setResponseCode(eventStatus).setBody(eventBody)
                }
            })
            server.start()
        }

        @AfterClass
        @JvmStatic
        fun stopServer() {
            server.shutdown()
        }
    }

    private var loaded = 0
    private val errors = mutableListOf<SegmentifyException>()

    private val callback = object : SegmentifyCallback<ArrayList<RecommendationModel>> {
        override fun onDataLoaded(data: ArrayList<RecommendationModel>) {
            loaded++
        }

        override fun onError(error: SegmentifyException) {
            errors.add(error)
        }
    }

    @After
    fun restoreClock() {
        SessionWindow.clock = { System.currentTimeMillis() }
    }

    @Before
    fun reset() {
        keyRequests.clear()
        events.clear()
        userAgents.clear()
        eventStatus = 200
        eventBody = "{\"statusCode\":\"SUCCESS\",\"responses\":[[]]}"
        keyStatus = 200
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

    private fun configureAndSendPageView() {
        val dataCenterUrl = server.url("/").toString().removeSuffix("/")
        SegmentifyManager.config(RuntimeEnvironment.getApplication(), "test-api-key", dataCenterUrl, "segmentify-sdk.test")
        val page = PageModel()
        page.category = "Home Page"
        SegmentifyManager.sendPageView(page, callback)
    }

    /** SFY-28787 */
    @Test
    fun pageViewSentRightAfterConfigOnFirstLaunchWaitsForTheIdsAndIsSentOnce() {
        configureAndSendPageView()

        await("the page view callback") { loaded == 1 }
        settle()

        assertEquals("key requests on first launch: $keyRequests", 1, keyRequests.size)
        assertTrue("asks for a user id and a session id: $keyRequests", keyRequests[0].contains("count=2"))
        assertEquals(1, events.size)
        assertEquals("PAGE_VIEW", events[0].get("name").asString)
        assertEquals("Home Page", events[0].get("category").asString)
        assertEquals("user-1", events[0].get("userId").asString)
        assertEquals("session-1", events[0].get("sessionId").asString)
        assertEquals(1, loaded)
        assertTrue(errors.isEmpty())
    }

    /** SFY-28788 */
    @Test
    fun serverErrorReachesTheHostAppThroughOnError() {
        eventStatus = 500

        configureAndSendPageView()

        await("the error callback") { errors.size == 1 }
        settle()

        assertEquals(1, errors.size)
        assertEquals(500, errors[0].httpStatus)
        assertEquals(0, loaded)
    }

    /** SFY-28788: the host learns why its event was given up. */
    @Test
    fun failedKeyRequestReachesTheHostAppWithItsStatus() {
        keyStatus = 503

        configureAndSendPageView()

        await("the error callback") { errors.size == 1 }
        settle()

        assertEquals(1, errors.size)
        assertEquals(503, errors[0].httpStatus)
        assertEquals(0, loaded)
        assertTrue("no event without ids: $events", events.isEmpty())
    }

    /** The backend rejects with HTTP 200; the host must still see an error. */
    @Test
    fun eventRejectedByTheBackendReachesTheHostAppWithTheReason() {
        eventBody = "{\"statusCode\":\"INVALID_EVENT\",\"timestamp\":1}"

        configureAndSendPageView()

        await("the error callback") { errors.size == 1 }
        settle()

        assertEquals("INVALID_EVENT", errors[0].statusCode)
        assertEquals(200, errors[0].httpStatus)
        assertEquals(0, loaded)
        assertEquals("a rejected event is not retried: $events", 1, events.size)
    }

    /** SFY-28912 */
    @Test
    fun keyAndEventRequestsCarryTheSdkUserAgent() {
        configureAndSendPageView()

        await("the page view callback") { loaded == 1 }

        val expected = SegmentifyUserAgent.current
        assertTrue(
            "unexpected header: $expected",
            Regex("^SegmentifySDK/" + Regex.escape(BuildConfig.SDK_VERSION) + " \\(android; Android [^;() ]+; [^;()]+\\) [^ /]+(/[^ /]+)?$").matches(expected)
        )
        assertEquals("one key request and one event: $userAgents", 2, userAgents.size)
        assertEquals(listOf<String?>(expected, expected), userAgents.toList())
    }

    /**
     * The logging interceptor must run after the one that adds the SDK's headers,
     * or the logged request shows neither User-Agent nor Origin.
     */
    @Test
    fun requestsAreLoggedWithTheHeadersTheSdkAdds() {
        configureAndSendPageView()
        await("the page view callback") { loaded == 1 }

        val interceptors = ConnectionManager.getSyncClient().interceptors()

        assertTrue("logging interceptor: $interceptors", interceptors.last() is HttpLoggingInterceptor)
        assertEquals("one logging interceptor: $interceptors", 1, interceptors.count { it is HttpLoggingInterceptor })
    }

    // SFY-28789: the session ends sessionKeepSeconds after the last event.

    private var now = 1_700_000_000_000L
    private val keepMillis = 86_400_000L

    private fun useTestClock() {
        SessionWindow.clock = { now }
    }

    private fun sendPageViewAndWait(expectedEvents: Int) {
        val page = PageModel()
        page.category = "Home Page"
        SegmentifyManager.sendPageView(page, callback)
        await("event $expectedEvents") { events.size == expectedEvents }
    }

    @Test
    fun sessionIsReusedInsideTheWindowWithoutAKeyRequest() {
        useTestClock()
        configureAndSendPageView()
        await("the first event") { events.size == 1 }

        now += keepMillis - 1_000
        sendPageViewAndWait(2)
        settle()

        assertEquals("one key request: $keyRequests", 1, keyRequests.size)
        assertEquals("session-1", events[1].get("sessionId").asString)
    }

    @Test
    fun sessionIdleLongerThanTheWindowIsReplacedOnTheNextEvent() {
        useTestClock()
        configureAndSendPageView()
        await("the first event") { events.size == 1 }

        // No config() in between: an app that stays alive must rotate too.
        now += keepMillis + 1_000
        sendPageViewAndWait(2)

        assertEquals(listOf("/get/key?count=2", "/get/key?count=1"), keyRequests.map { it.replace("&", "") })
        assertEquals("session-2", events[1].get("sessionId").asString)
        assertEquals("user-1", events[1].get("userId").asString)
    }

    @Test
    fun everyEventKeepsTheSessionAlive() {
        useTestClock()
        configureAndSendPageView()
        await("the first event") { events.size == 1 }

        now += keepMillis * 8 / 10
        sendPageViewAndWait(2)
        now += keepMillis * 8 / 10
        sendPageViewAndWait(3)
        settle()

        assertEquals("one key request: $keyRequests", 1, keyRequests.size)
        assertEquals(listOf("session-1", "session-1", "session-1"), events.map { it.get("sessionId").asString })
    }

    /** Before the fix the session lasted twice the configured time, and only config() checked it. */
    @Test
    fun configAfterTheWindowStartsANewSession() {
        useTestClock()
        configureAndSendPageView()
        await("the first event") { events.size == 1 }

        now += keepMillis + 1_000
        configureAndSendPageView()
        await("the second event") { events.size == 2 }

        assertEquals("session-2", events[1].get("sessionId").asString)
    }

    @Test
    fun storedSessionWithoutAnEndIsNotUsed() {
        useTestClock()
        configureAndSendPageView()
        await("the first event") { events.size == 1 }
        SegmentifyManager.clientPreferences!!.setSavedDateforSession(0)

        sendPageViewAndWait(2)

        assertEquals("session-2", events[1].get("sessionId").asString)
    }
}
