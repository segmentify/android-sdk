package com.segmentify.segmentifyandroidsdk

import com.segmentify.segmentifyandroidsdk.utils.SegmentifyUserAgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SFY-28912: the format of the User-Agent header the backend parses. */
class SegmentifyUserAgentTest {

    @Test
    fun format() {
        assertEquals(
            "SegmentifySDK/2.2.9 (android; Android 14; Pixel 8) com.acme.shop/2.3.1",
            SegmentifyUserAgent.make("2.2.9", "14", "Pixel 8", "com.acme.shop", "2.3.1")
        )
    }

    @Test
    fun partsThatAreNotKnownAreLeftOut() {
        assertEquals("SegmentifySDK/2.2.9 (android; Android 14)", SegmentifyUserAgent.make("2.2.9", "14", null, null, null))
        assertEquals("SegmentifySDK/2.2.9 (android; Android 14) com.acme.shop", SegmentifyUserAgent.make("2.2.9", "14", " ", "com.acme.shop", null))
        assertEquals("SegmentifySDK/2.2.9 (android; Android 14; Pixel 8)", SegmentifyUserAgent.make("2.2.9", "14", "Pixel 8", null, "2.3.1"))
    }

    /** The backend needs a version after "Android"; a device that reports none still sends a header it can parse. */
    @Test
    fun missingVersionsAreSentAsUnknown() {
        assertEquals("SegmentifySDK/unknown (android; Android unknown)", SegmentifyUserAgent.make(null, "", null, null, null))
    }

    /** The backend splits on ; ( ) and spaces, and OkHttp refuses a header that is not ASCII. */
    @Test
    fun charactersThatWouldBreakTheFormatAreRemoved() {
        val header = SegmentifyUserAgent.make("2.2.9 beta", "14 (U)", "Galaxy;S24 Ayşe\n", "com.acme/shop app", "2.3.1 (45)")

        assertEquals("SegmentifySDK/2.2.9beta (android; Android 14U; GalaxyS24 Aye) com.acmeshopapp/2.3.145", header)
        assertTrue(header.all { it.code in 0x20..0x7E })
    }
}
