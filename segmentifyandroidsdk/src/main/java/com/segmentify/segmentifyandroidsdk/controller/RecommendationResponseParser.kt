package com.segmentify.segmentifyandroidsdk.controller

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.segmentify.segmentifyandroidsdk.model.DynamicItemsModel
import com.segmentify.segmentifyandroidsdk.model.EventResponseModel
import com.segmentify.segmentifyandroidsdk.model.ParamsModel
import com.segmentify.segmentifyandroidsdk.model.ProductRecommendationModel
import com.segmentify.segmentifyandroidsdk.model.RecommendationModel
import com.segmentify.segmentifyandroidsdk.utils.SegmentifyLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns the raw event response into the recommendation models handed to the
 * host app. Kept free of networking so it can be unit tested on the JVM; the
 * caller decides what to do with impressions through [onImpression].
 */
internal object RecommendationResponseParser {

    private const val RECOMMEND_PRODUCTS_TYPE = "recommendProducts"
    private const val STATIC_ITEMS_SOURCE = "RECOMMENDATION_SOURCE_STATIC_ITEMS"

    fun parse(
        eventResponseModel: EventResponseModel?,
        onImpression: (instanceId: String?, actionId: String?) -> Unit
    ): ArrayList<RecommendationModel> {

        val recommendations = ArrayList<RecommendationModel>()
        val responses = eventResponseModel?.responses?.firstOrNull() ?: return recommendations
        val seenProducts = arrayListOf<ProductRecommendationModel>()

        for (response in responses) {
            // Anything that is not a product recommendation (engagement campaigns,
            // banners, future types) is not ours to parse; skip it instead of failing.
            val params = response?.params ?: continue
            if (!isRecommendation(response.type, params)) {
                continue
            }

            val recommendationModel = RecommendationModel()
            recommendationModel.notificationTitle = params.notificationTitle
            recommendationModel.instanceId = params.instanceId
            recommendationModel.actionId = params.actionId

            onImpression(recommendationModel.instanceId, recommendationModel.actionId)

            val recommendedProducts = toJsonObject(params.recommendedProducts)

            val staticProducts = recommendedProducts?.optJSONArray(STATIC_ITEMS_SOURCE)
            if (staticProducts != null) {
                for (i in 0 until staticProducts.length()) {
                    val product = parseProductOrNull(staticProducts, i) ?: continue
                    recommendationModel.products?.add(product)
                }
            }

            for (dynamicItem in parseDynamicItems(params.dynamicItems)) {
                val source = sourceKey(dynamicItem) ?: continue
                val products = recommendedProducts?.optJSONArray(source) ?: continue
                val itemCount = dynamicItem.itemCount

                // Counting rule kept as it was: a duplicate gives one slot back.
                var added = 0
                for (k in 0 until products.length()) {
                    val product = parseProductOrNull(products, k)
                    if (product != null) {
                        if (!isProductSeen(seenProducts, product)) {
                            recommendationModel.products?.add(product)
                            added = added + 1
                        } else if (added > 0) {
                            added = added - 1
                        }
                    }

                    if (itemCount != null && added == itemCount) {
                        break
                    }
                }
            }

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

    private fun toJsonObject(value: Any?): JSONObject? {
        if (value == null) {
            return null
        }
        return try {
            JSONObject(Gson().toJson(value))
        } catch (ex: Exception) {
            SegmentifyLogger.printErrorLog("recommendedProducts is not a JSON object: " + ex.message)
            null
        }
    }

    private fun parseDynamicItems(json: String?): List<DynamicItemsModel> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val listType = object : TypeToken<ArrayList<DynamicItemsModel?>>() {}.type
            val items: ArrayList<DynamicItemsModel?>? = Gson().fromJson(json, listType)
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

    private fun parseProductOrNull(products: JSONArray, index: Int): ProductRecommendationModel? {
        return try {
            parseJsonToProductModel(products.getJSONObject(index))
        } catch (ex: Exception) {
            SegmentifyLogger.printErrorLog("skipping malformed product: " + ex.message)
            null
        }
    }

    private fun parseJsonToProductModel(productJson : JSONObject) : ProductRecommendationModel {
        var productRecommendationModel = ProductRecommendationModel()

        if(productJson.has("productId")){
            productRecommendationModel.productId = productJson.getString("productId")
        }
        if(productJson.has("name")){
            productRecommendationModel.name = productJson.getString("name")
        }
        if(productJson.has("url")){
            productRecommendationModel.url = productJson.getString("url")
        }
        if(productJson.has("image")){
            productRecommendationModel.image = productJson.getString("image")
        }
        if(productJson.has("price")){
            productRecommendationModel.price = productJson.getDouble("price")
        }
        if(productJson.has("priceText")){
            productRecommendationModel.priceText = productJson.getString("priceText")
        }
        if(productJson.has("oldPriceText")){
            productRecommendationModel.oldPriceText = productJson.getString("oldPriceText")
        }
        if(productJson.has("specialPrice")){
            productRecommendationModel.specialPrice = productJson.getDouble("specialPrice")
        }
        if(productJson.has("specialPriceText")){
            productRecommendationModel.specialPriceText = productJson.getString("specialPriceText")
        }
        if(productJson.has("categories")) {
            val categoriesJSONArray = productJson.getJSONArray("categories")
            val categories = ArrayList<String>()
            if(categoriesJSONArray != null) {
                for(i in 0 until categoriesJSONArray.length()) {
                    categories.add(categoriesJSONArray.getString(i))
                }
            }
            productRecommendationModel.categories = categories
        }
        if(productJson.has("language")){
            productRecommendationModel.language = productJson.getString("language")
        }
        if(productJson.has("currency")){
            productRecommendationModel.currency = productJson.getString("currency")
        }
        if(productJson.has("inStock")) {
            productRecommendationModel.inStock = productJson.getBoolean("inStock")
        }
        if(productJson.has("brand")){
            productRecommendationModel.brand = productJson.getString("brand")
        }
        if(productJson.has("oldPrice")){
            productRecommendationModel.oldPrice = productJson.getDouble("oldPrice")
        }
        if(productJson.has("mUrl")){
            productRecommendationModel.mUrl = productJson.getString("mUrl")
        }
        if(productJson.has("imageXS")) {
            productRecommendationModel.imageXS = productJson.getString("imageXS")
        }
        if(productJson.has("imageS")) {
            productRecommendationModel.imageS = productJson.getString("imageS")
        }
        if(productJson.has("imageM")){
            productRecommendationModel.imageM = productJson.getString("imageM")
        }
        if(productJson.has("imageL")){
            productRecommendationModel.imageL = productJson.getString("imageL")
        }
        if(productJson.has("imageXL")){
            productRecommendationModel.imageXL = productJson.getString("imageXL")
        }
        if(productJson.has("additionalImages")){
           val additionalImagesJSONArray = productJson.getJSONArray("additionalImages")
            val additionalImages = ArrayList<String>()
            if(additionalImagesJSONArray != null){
                for(i in 0 until additionalImagesJSONArray.length()) {
                    additionalImages.add(additionalImagesJSONArray.getString(i))
                }
            }
            productRecommendationModel.additionalImages = additionalImages
        }

        if(productJson.has("category")) {
            val categoryJSONArray = productJson.getJSONArray("category")
            val category = ArrayList<String>()
            if(categoryJSONArray != null) {
                for(i in 0 until categoryJSONArray!!.length()) {
                    category.add(categoryJSONArray.getString(i))
                }
            }
            productRecommendationModel.category = category
        }
        if(productJson.has("mainCategory")){
            productRecommendationModel.mainCategory = productJson.getString("mainCategory")
        }
        if(productJson.has("gender")){
            productRecommendationModel.gender = productJson.getString("gender")
        }

        if(productJson.has("colors")){
            val colorsJSONArray = productJson.getJSONArray("colors")
            val colors = ArrayList<String>()
            if(colorsJSONArray != null){
                for(i in 0 until colorsJSONArray.length()) {
                    colors.add(colorsJSONArray.getString(i))
                }
            }
            productRecommendationModel.colors = colors
        }

        if(productJson.has("sizes")){
            val sizesJSONArray = productJson.getJSONArray("sizes")
            val sizes = ArrayList<String>()

            if(sizesJSONArray != null){
                for(i in 0 until sizesJSONArray.length()) {
                    sizes.add(sizesJSONArray.getString(i))
                }
            }
            productRecommendationModel.sizes = sizes
        }

        if(productJson.has("allSizes")){
            val allSizesJSONArray = productJson.getJSONArray("allSizes")
            val allSizes = ArrayList<String>()
            if(allSizesJSONArray != null) {
                for (i in 0 until allSizesJSONArray!!.length()) {
                    allSizes.add(allSizesJSONArray.getString(i))
                }
            }
            productRecommendationModel.allSizes = allSizes
        }

        if(productJson.has("labels")){
            var labelsJSONArray = productJson.getJSONArray("labels")
            var labels = ArrayList<String>()
            if(labelsJSONArray != null){
                for(i in 0 until labelsJSONArray!!.length()) {
                    labels.add(labelsJSONArray.getString(i))
                }
            }
            productRecommendationModel.labels = labels
        }

        if(productJson.has("noUpdate")){
            productRecommendationModel.noUpdate = productJson.getBoolean("noUpdate")
        }



        if(productJson.has("params")){
            val obj =  productJson.getString("params");
            val paramsMap: Map<String, Any> =Gson().fromJson(obj, object : TypeToken<Map<String, Any>>() {}.type)
            productRecommendationModel.params =paramsMap
        }

        if(productJson.has("paramsList")){
            val paramsListJSONObject = productJson.getJSONObject("paramsList")
            val paramsList = HashMap<String, List<String>>()

            if(paramsListJSONObject != null) {
                val keys = paramsListJSONObject.keys()
                while(keys.hasNext()) {
                    val key = keys.next()
                    val valueArray = paramsListJSONObject.getJSONArray(key)
                    val valueList = ArrayList<String>()

                    for (i in 0 until valueArray.length()) {
                        valueList.add(valueArray.getString(i))
                    }

                    paramsList[key] = valueList
                }
            }

            productRecommendationModel.paramsList = paramsList
        }

        if(productJson.has("lastUpdateTime")){
            productRecommendationModel.lastUpdateTime = productJson.getLong("lastUpdateTime")
        }

        if(productJson.has("stockCount")){
            productRecommendationModel.stockCount = productJson.getInt("stockCount")
        }

        if(productJson.has("stockRatio")) {
            productRecommendationModel.stockRatio = productJson.getDouble("stockRatio").toFloat()
        }

        if(productJson.has("stockStatus")) {
            productRecommendationModel.stockStatus = productJson.getInt("stockStatus")
        }

        if(productJson.has("publishTime")) {
            productRecommendationModel.publishTime = productJson.getLong("publishTime")
        }

        if(productJson.has("combineIds")) {
            val combineIdsJSONArray = productJson.getJSONArray("combineIds")
            val combineIds = ArrayList<String>()

            if (combineIdsJSONArray != null) {
                for (i in 0 until combineIdsJSONArray.length()) {
                    combineIds.add(combineIdsJSONArray.getString(i))
                }
                productRecommendationModel.combineIds = combineIds
            }
        }

        if (productJson.has("groupId")) {
            productRecommendationModel.groupId = productJson.getString("groupId")
        }

        if (productJson.has("priceSegment")) {
            productRecommendationModel.priceSegment = productJson.getString("priceSegment")
        }

        if (productJson.has("lastBoughtTime")) {
            productRecommendationModel.lastBoughtTime = productJson.getLong("lastBoughtTime")

        }

        if (productJson.has("scoreCount")) {
            productRecommendationModel.scoreCount = productJson.getInt("scoreCount")
        }

        if (productJson.has("reviewCount")) {
            productRecommendationModel.reviewCount = productJson.getInt("reviewCount")
        }

        if (productJson.has("savingOverTime")) {
            productRecommendationModel.savingOverTime = productJson.getLong("savingOverTime")
        }






        return productRecommendationModel
    }

    private fun isProductSeen(seenProducts: ArrayList<ProductRecommendationModel>, productRecommendationModel: ProductRecommendationModel) : Boolean {
        for(productModel in seenProducts){
            if(productModel.productId == productRecommendationModel.productId){
                return true
            }
        }
        seenProducts.add(productRecommendationModel)
        return false
    }
}
