package com.segmentify.segmentifyandroidsdk

import com.segmentify.segmentifyandroidsdk.model.CustomEventModel
import com.segmentify.segmentifyandroidsdk.model.PageModel
import com.segmentify.segmentifyandroidsdk.model.ProductModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyCallback
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SFY-28788: a call rejected before any request is made must still answer its
 * callback. Needs no network and no config(): the checks run first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ValidationErrorTest {

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

    private fun assertRejectedOnce() {
        assertEquals(1, errors.size)
        assertNull(errors[0].httpStatus)
        assertEquals(0, loaded)
    }

    @Test
    fun pageViewWithoutCategoryReportsAnError() {
        SegmentifyManager.sendPageView(PageModel(), callback)

        assertRejectedOnce()
    }

    @Test
    fun productViewWithoutProductIdReportsAnError() {
        SegmentifyManager.sendProductView(ProductModel(), callback)

        assertRejectedOnce()
    }

    @Test
    fun customEventWithoutTypeReportsAnError() {
        SegmentifyManager.sendCustomEvent(CustomEventModel(), callback)

        assertRejectedOnce()
    }
}
