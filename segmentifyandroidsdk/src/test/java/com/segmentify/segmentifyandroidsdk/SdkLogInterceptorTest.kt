package com.segmentify.segmentifyandroidsdk

import com.segmentify.segmentifyandroidsdk.network.SdkLogInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** SFY-28790: logStatus is read for every request, not once when the client is built. */
class SdkLogInterceptorTest {

    private val server = MockWebServer()
    private val lines = CopyOnWriteArrayList<String>()

    @Volatile
    private var visible = false

    private val client = OkHttpClient.Builder()
        .addInterceptor(SdkLogInterceptor({ visible }, { lines.add(it) }))
        .build()

    @Before
    fun start() {
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun get() {
        server.enqueue(MockResponse().setBody("ok"))
        client.newCall(Request.Builder().url(server.url("/event")).build()).execute().close()
    }

    @Test
    fun logsNothingWhileLoggingIsOff() {
        get()

        assertTrue("logged while off: $lines", lines.isEmpty())
    }

    @Test
    fun loggingTurnedOnAfterTheFirstRequestLogsTheNextOne() {
        get()
        visible = true
        get()

        assertTrue("the second request was not logged: $lines", lines.any { it.startsWith("--> GET") && it.contains("/event") })
    }

    @Test
    fun loggingTurnedOffStopsLogging() {
        visible = true
        get()
        visible = false
        lines.clear()
        get()

        assertTrue("logged after it was turned off: $lines", lines.isEmpty())
    }
}
