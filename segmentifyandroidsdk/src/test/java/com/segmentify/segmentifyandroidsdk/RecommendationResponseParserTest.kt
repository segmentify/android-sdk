package com.segmentify.segmentifyandroidsdk

import com.google.gson.Gson
import com.segmentify.segmentifyandroidsdk.controller.RecommendationResponseParser
import com.segmentify.segmentifyandroidsdk.model.EventResponseModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationResponseParserTest {

    private val gson = Gson()
    private val impressions = mutableListOf<Pair<String?, String?>>()

    private fun parse(json: String): ArrayList<RecommendationModel> {
        val response = gson.fromJson(json, EventResponseModel::class.java)
        return RecommendationResponseParser.parse(response) { instanceId, actionId ->
            impressions.add(instanceId to actionId)
        }
    }

    private fun productIds(model: RecommendationModel): List<String?> {
        return model.products!!.map { it.productId }
    }

    private val dynamicSource = "RECOMMENDATION_SOURCE_ALL_PRODUCTS|TIME_FRAME_THIS_WEEK|SCORE_VIEW"

    private val dynamicItems =
        "[{\\\"recommendationSource\\\":\\\"RECOMMENDATION_SOURCE_ALL_PRODUCTS\\\"," +
            "\\\"timeFrame\\\":\\\"TIME_FRAME_THIS_WEEK\\\",\\\"score\\\":\\\"SCORE_VIEW\\\"," +
            "\\\"itemCount\\\":\\\"2\\\"}]"

    private val dynamicItemsWithoutCount =
        "[{\\\"recommendationSource\\\":\\\"RECOMMENDATION_SOURCE_ALL_PRODUCTS\\\"," +
            "\\\"timeFrame\\\":\\\"TIME_FRAME_THIS_WEEK\\\",\\\"score\\\":\\\"SCORE_VIEW\\\"}]"

    private val threeProducts =
        """[{"productId":"p1","name":"One","price":10.5},{"productId":"p2","name":"Two"},{"productId":"p3","name":"Three"}]"""

    private fun recommendation(
        dynamicItemsJson: String? = dynamicItems,
        recommendedProducts: String = """{"$dynamicSource":$threeProducts}""",
        type: String = "recommendProducts"
    ): String {
        val dynamic = if (dynamicItemsJson == null) "" else """"dynamicItems":"$dynamicItemsJson","""
        return """{"type":"$type","params":{"notificationTitle":"For you","instanceId":"scn_1","actionId":"act_1",$dynamic"recommendedProducts":$recommendedProducts}}"""
    }

    private fun envelope(vararg items: String): String {
        return """{"responses":[[${items.joinToString(",")}]]}"""
    }

    // Characterization: the behaviour that must not change.

    @Test
    fun recommendationWithDynamicItemsIsParsedAndLimitedToItemCount() {
        val result = parse(envelope(recommendation()))

        assertEquals(1, result.size)
        assertEquals("For you", result[0].notificationTitle)
        assertEquals("scn_1", result[0].instanceId)
        assertEquals("act_1", result[0].actionId)
        assertEquals(listOf("p1", "p2"), productIds(result[0]))
        assertEquals(10.5, result[0].products!![0].price!!, 0.0)
        assertEquals(listOf<Pair<String?, String?>>("scn_1" to "act_1"), impressions)
    }

    @Test
    fun emptyResponsesReturnEmptyList() {
        assertTrue(parse("""{"responses":[]}""").isEmpty())
        assertTrue(parse("""{}""").isEmpty())
        assertTrue(impressions.isEmpty())
    }

    // SFY-28786: each of these crashed the host app.

    @Test
    fun responseOfAnotherTypeIsSkipped() {
        val popup = """{"type":"popupBanner","params":{"instanceId":"scn_9","actionId":"act_9","html":"<div></div>"}}"""

        val result = parse(envelope(popup))

        assertTrue(result.isEmpty())
        assertTrue(impressions.isEmpty())
    }

    @Test
    fun mixedResponseKeepsOnlyTheRecommendation() {
        val popup = """{"type":"popupBanner","params":{"instanceId":"scn_9","actionId":"act_9"}}"""

        val result = parse(envelope(popup, recommendation()))

        assertEquals(1, result.size)
        assertEquals("scn_1", result[0].instanceId)
        assertEquals(listOf<Pair<String?, String?>>("scn_1" to "act_1"), impressions)
    }

    @Test
    fun recommendationWithoutDynamicItemsKeepsStaticProducts() {
        val staticOnly = recommendation(
            dynamicItemsJson = null,
            recommendedProducts = """{"RECOMMENDATION_SOURCE_STATIC_ITEMS":[{"productId":"s1","name":"Static"}]}"""
        )

        val result = parse(envelope(staticOnly))

        assertEquals(1, result.size)
        assertEquals(listOf("s1"), productIds(result[0]))
    }

    @Test
    fun dynamicSourceWithoutProductsYieldsNoProducts() {
        val missingSource = recommendation(recommendedProducts = """{"SOME_OTHER_SOURCE":$threeProducts}""")

        val result = parse(envelope(missingSource))

        assertEquals(1, result.size)
        assertTrue(result[0].products!!.isEmpty())
    }

    @Test
    fun dynamicItemWithoutItemCountReturnsAllProducts() {
        val result = parse(envelope(recommendation(dynamicItemsJson = dynamicItemsWithoutCount)))

        assertEquals(listOf("p1", "p2", "p3"), productIds(result[0]))
    }

    @Test
    fun responseItemWithoutParamsIsSkipped() {
        val result = parse(envelope("""{"type":"recommendProducts"}"""))

        assertTrue(result.isEmpty())
        assertTrue(impressions.isEmpty())
    }
}
