package com.segmentify.segmentifyandroidsdk.controller

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.segmentify.segmentifyandroidsdk.model.DynamicItemsModel
import com.segmentify.segmentifyandroidsdk.model.EventResponseModel
import com.segmentify.segmentifyandroidsdk.model.ParamsModel
import com.segmentify.segmentifyandroidsdk.model.ProductRecommendationModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger

/**
 * Turns the raw event response into the recommendation models handed to the
 * host app. Kept free of networking so it can be unit tested on the JVM; the
 * caller decides what to do with impressions through [onImpression].
 *
 * Gson is the only JSON library used here, so the parser behaves the same on
 * a device and in JVM unit tests.
 */
internal object RecommendationResponseParser {

    private const val RECOMMEND_PRODUCTS_TYPE = "recommendProducts"
    private const val STATIC_ITEMS_SOURCE = "RECOMMENDATION_SOURCE_STATIC_ITEMS"

    private val gson = Gson()
    private val dynamicItemsType = object : TypeToken<ArrayList<DynamicItemsModel?>>() {}.type

    fun parse(
        eventResponseModel: EventResponseModel?,
        onImpression: (instanceId: String?, actionId: String?) -> Unit
    ): ArrayList<RecommendationModel> {

        val recommendations = ArrayList<RecommendationModel>()
        // The backend returns one inner list per event in the request (responses[i]
        // belongs to event i). The SDK sends one event per request, so only the
        // first list can exist.
        val responses = eventResponseModel?.responses?.firstOrNull() ?: return recommendations
        // Product ids already handed out in this response, across all of its widgets.
        val seenProductIds = HashSet<String?>()

        for (response in responses) {
            // Anything that is not a product recommendation (promotions, popups,
            // future types) is not ours to parse; skip it instead of failing.
            val params = response?.params ?: continue
            if (!isRecommendation(response.type, params)) {
                continue
            }

            val recommendationModel = RecommendationModel()
            recommendationModel.notificationTitle = params.notificationTitle
            recommendationModel.instanceId = params.instanceId
            recommendationModel.actionId = params.actionId

            onImpression(recommendationModel.instanceId, recommendationModel.actionId)

            val sources = toJsonObject(params.recommendedProducts)
            val products = ArrayList<ProductRecommendationModel>()

            // Static items are pinned by the merchandiser: always shown, and
            // remembered so that a dynamic source does not repeat them.
            val staticProducts = productsOf(sources, STATIC_ITEMS_SOURCE)
            staticProducts.forEach { seenProductIds.add(it.productId) }
            products.addAll(staticProducts)

            for (dynamicItem in parseDynamicItems(params.dynamicItems)) {
                val source = sourceKey(dynamicItem) ?: continue
                products.addAll(takeUnseen(productsOf(sources, source), dynamicItem.itemCount, seenProductIds))
            }

            recommendationModel.products = products
            recommendations.add(recommendationModel)
        }

        return recommendations
    }

    private fun isRecommendation(type: String?, params: ParamsModel): Boolean {
        if (type == null) {
            return params.recommendedProducts != null
        }
        return type == RECOMMEND_PRODUCTS_TYPE
    }

    /** Up to [itemCount] products that were not handed out before; no limit when it is missing or not positive. */
    private fun takeUnseen(
        candidates: List<ProductRecommendationModel>,
        itemCount: Int?,
        seenProductIds: MutableSet<String?>
    ): List<ProductRecommendationModel> {
        val limit = if (itemCount != null && itemCount > 0) itemCount else Int.MAX_VALUE
        val taken = ArrayList<ProductRecommendationModel>()
        for (product in candidates) {
            if (taken.size == limit) {
                break
            }
            if (seenProductIds.add(product.productId)) {
                taken.add(product)
            }
        }
        return taken
    }

    private fun toJsonObject(value: Any?): JsonObject? {
        if (value == null) {
            return null
        }
        return try {
            val element = gson.toJsonTree(value)
            if (element.isJsonObject) element.asJsonObject else null
        } catch (ex: Exception) {
            SegmentifyLogger.printErrorLog("recommendedProducts is not a JSON object: " + ex.message)
            null
        }
    }

    private fun productsOf(sources: JsonObject?, source: String): List<ProductRecommendationModel> {
        val products = sources?.get(source)
        if (products == null || !products.isJsonArray) {
            return emptyList()
        }
        return products.asJsonArray.mapNotNull { parseProductOrNull(it) }
    }

    private fun parseProductOrNull(product: JsonElement): ProductRecommendationModel? {
        if (!product.isJsonObject) {
            return null
        }
        return try {
            gson.fromJson(product, ProductRecommendationModel::class.java)
        } catch (ex: Exception) {
            SegmentifyLogger.printErrorLog("skipping malformed product: " + ex.message)
            null
        }
    }

    private fun parseDynamicItems(json: String?): List<DynamicItemsModel> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val items: ArrayList<DynamicItemsModel?>? = gson.fromJson(json, dynamicItemsType)
            items?.filterNotNull() ?: emptyList()
        } catch (ex: Exception) {
            SegmentifyLogger.printErrorLog("dynamicItems could not be parsed: " + ex.message)
            emptyList()
        }
    }

    private fun sourceKey(dynamicItem: DynamicItemsModel): String? {
        val source = dynamicItem.recommendationSource
        if (source.isNullOrEmpty()) {
            return null
        }
        var key: String = source
        if (!dynamicItem.timeFrame.isNullOrEmpty()) {
            key = key + "|" + dynamicItem.timeFrame
            if (!dynamicItem.score.isNullOrEmpty()) {
                key = key + "|" + dynamicItem.score
            }
        }
        return key
    }
}
