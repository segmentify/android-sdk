package com.segmentify.segmentifyandroidsdk

import android.os.Looper
import com.segmentify.segmentifyandroidsdk.model.NotificationModel
import com.segmentify.segmentifyandroidsdk.model.NotificationType
import com.segmentify.segmentifyandroidsdk.model.PageModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
 * SFY-28790: the SDK used before config() or configured in an unexpected
 * order. Nothing here may throw into the host app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConfigurationStateTest {

    companion object {
        private val server = MockWebServer()
        private val paths = CopyOnWriteArrayList<String>()

        @BeforeClass
        @JvmStatic
        fun startServer() {
            server.setDispatcher(object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path ?: ""
                    paths.add(path)
                    if (path.startsWith("/get/key")) {
                        val keys = if (path.contains("count=2")) "[\"user-1\",\"session-1\"]" else "[\"session-1\"]"
                        return MockResponse().setBody(keys)
                    }
                    if (path.startsWith("/native/")) {
                        return MockResponse().setBody("{}")
                    }
                    return MockResponse().setBody("{\"statusCode\":\"SUCCESS\",\"responses\":[[]]}")
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

    private val dataCenterUrl get() = server.url("/").toString().removeSuffix("/")

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

    /** Every scenario starts with an SDK that was never configured in this process. */
    @Before
    fun unconfigured() {
        paths.clear()
        SegmentifyManager.clientPreferences = null
        SegmentifyManager.configModel.dataCenterUrlPush = null
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue("timed out waiting for $what", condition())
    }

    private fun settle() {
        val until = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < until) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }

    private fun page(): PageModel {
        val page = PageModel()
        page.category = "Home Page"
        return page
    }

    @Test
    fun eventBeforeConfigFailsThroughOnError() {
        SegmentifyManager.sendPageView(page(), callback)

        await("the error callback") { errors.size == 1 }
        assertTrue(errors[0].message!!, errors[0].message!!.contains("config"))
        assertEquals(0, loaded)
    }

    @Test
    fun eventCarryingItsOwnIdsBeforeConfigFailsThroughOnErrorAndIsNotSent() {
        val page = page()
        page.userId = "host-user"
        page.sessionId = "host-session"

        SegmentifyManager.sendPageView(page, callback)

        await("the error callback") { errors.size == 1 }
        settle()
        assertTrue(errors[0].message!!, errors[0].message!!.contains("config"))
        assertTrue("sent without config: $paths", paths.isEmpty())
    }

    @Test
    fun setConfigBeforeConfigDoesNotThrow() {
        SegmentifyManager.setConfig("test-api-key", dataCenterUrl, "segmentify-sdk.test")
    }

    @Test
    fun pushBeforeConfigIsDroppedWithoutThrowing() {
        val notification = NotificationModel()
        notification.type = NotificationType.PERMISSION_INFO
        notification.deviceToken = "device-token"

        SegmentifyManager.sendNotification(notification)
        settle()

        assertTrue("sent without config: $paths", paths.isEmpty())
    }

    @Test
    fun pushConfiguredAfterTheFirstRequestStillReachesThePushServer() {
        SegmentifyManager.config(RuntimeEnvironment.getApplication(), "test-api-key", dataCenterUrl, "segmentify-sdk.test")
        SegmentifyManager.sendPageView(page(), callback)
        await("the page view") { loaded == 1 }

        SegmentifyManager.setPushConfig(dataCenterUrl)
        val notification = NotificationModel()
        notification.type = NotificationType.PERMISSION_INFO
        notification.deviceToken = "device-token"
        SegmentifyManager.sendNotification(notification)

        await("the push subscription request: $paths") { paths.any { it.startsWith("/native/subscription/push") } }
    }

    @Test
    fun eventsGoToTheDataCenterOfTheLatestConfig() {
        SegmentifyManager.config(RuntimeEnvironment.getApplication(), "test-api-key", dataCenterUrl, "segmentify-sdk.test")
        SegmentifyManager.sendPageView(page(), callback)

        await("the page view on this test's server: $paths") { loaded == 1 }
        assertTrue("no event reached this test's server: $paths", paths.any { it.startsWith("/add/events") })
    }
}
