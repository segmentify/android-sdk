package com.segmentify.segmentifyandroidsdk

import com.segmentify.segmentifyandroidsdk.controller.PendingEventQueue
import com.segmentify.segmentifyandroidsdk.controller.SessionIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SFY-28787: events sent before the user id and session id arrive must wait, not vanish. */
class PendingEventQueueTest {

    private var currentIds: SessionIds? = null
    private var idRequests = 0
    private val sent = mutableListOf<String>()
    private val logs = mutableListOf<String>()

    private fun queue(maxPending: Int = PendingEventQueue.DEFAULT_MAX_PENDING) = PendingEventQueue(
        ids = { currentIds },
        requestIds = { idRequests++ },
        log = { logs.add(it) },
        maxPending = maxPending
    )

    private fun event(name: String): (SessionIds) -> Unit = { ids ->
        sent.add("$name:${ids.userId}:${ids.sessionId}")
    }

    @Test
    fun eventIsSentImmediatelyWhenIdsAreKnown() {
        currentIds = SessionIds("u1", "s1")

        queue().submit(event("pageView"))

        assertEquals(listOf("pageView:u1:s1"), sent)
        assertEquals(0, idRequests)
    }

    @Test
    fun eventWaitsForIdsAndIsSentExactlyOnce() {
        val queue = queue()

        queue.submit(event("pageView"))
        assertTrue(sent.isEmpty())
        assertEquals(1, idRequests)

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()
        queue.onIdsAvailable()

        assertEquals(listOf("pageView:u1:s1"), sent)
    }

    @Test
    fun waitingEventsShareOneIdRequestAndKeepTheirOrder() {
        val queue = queue()

        queue.submit(event("pageView"))
        queue.submit(event("productView"))
        queue.submit(event("basket"))
        assertEquals(1, idRequests)

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()

        assertEquals(listOf("pageView:u1:s1", "productView:u1:s1", "basket:u1:s1"), sent)
    }

    @Test
    fun ensureIdsDoesNotStartASecondRequestWhileOneIsRunning() {
        val queue = queue()

        queue.ensureIds()
        queue.submit(event("pageView"))
        queue.ensureIds()

        assertEquals(1, idRequests)
    }

    @Test
    fun ensureIdsDoesNothingWhenIdsAreKnown() {
        currentIds = SessionIds("u1", "s1")

        queue().ensureIds()

        assertEquals(0, idRequests)
    }

    @Test
    fun failedIdRequestKeepsEventsAndIsRetriedByTheNextEvent() {
        val queue = queue()

        queue.submit(event("pageView"))
        queue.onIdsFailed()
        assertTrue(sent.isEmpty())

        queue.submit(event("productView"))
        assertEquals(2, idRequests)

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()

        assertEquals(listOf("pageView:u1:s1", "productView:u1:s1"), sent)
    }

    @Test
    fun idsReportedAvailableButStillMissingKeepEventsWaiting() {
        val queue = queue()

        queue.submit(event("pageView"))
        queue.onIdsAvailable()
        assertTrue(sent.isEmpty())

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()

        assertEquals(listOf("pageView:u1:s1"), sent)
    }

    @Test
    fun oldestEventIsDroppedAndLoggedWhenTheQueueIsFull() {
        val queue = queue(maxPending = 2)

        queue.submit(event("first"))
        queue.submit(event("second"))
        queue.submit(event("third"))
        assertEquals(1, logs.size)

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()

        assertEquals(listOf("second:u1:s1", "third:u1:s1"), sent)
    }

    @Test
    fun eventSubmittedWhileFlushingIsSentAfterTheWaitingOnes() {
        val queue = queue()
        queue.submit { ids ->
            sent.add("first:${ids.userId}")
            queue.submit(event("nested"))
        }

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()

        assertEquals(listOf("first:u1", "nested:u1:s1"), sent)
    }

    // Code review follow-ups.

    @Test
    fun throwingEventDoesNotStopTheOthersAndDoesNotEscape() {
        val queue = queue()
        queue.submit { throw IllegalStateException("boom") }
        queue.submit(event("second"))

        currentIds = SessionIds("u1", "s1")
        queue.onIdsAvailable()

        assertEquals(listOf("second:u1:s1"), sent)
        assertEquals(1, logs.size)
    }

    @Test
    fun idsArrivingBetweenCheckAndEnqueueSendTheEventWithoutANewRequest() {
        // The first read sees no ids; by the time the queue takes its lock the
        // key response has stored them and onIdsAvailable() has already run.
        var reads = 0
        val queue = PendingEventQueue(
            ids = { if (reads++ == 0) null else SessionIds("u1", "s1") },
            requestIds = { idRequests++ },
            log = { logs.add(it) }
        )

        queue.submit(event("pageView"))

        assertEquals(listOf("pageView:u1:s1"), sent)
        assertEquals(0, idRequests)
    }
}
