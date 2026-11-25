package com.segmentify.segmentifyandroidsdk.controller

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.segmentify.segmentifyandroidsdk.model.DynamicItemsModel
import com.segmentify.segmentifyandroidsdk.model.EventResponseModel
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

    fun parse(
        eventResponseModel: EventResponseModel?,
        onImpression: (instanceId: String?, actionId: String?) -> Unit
    ): ArrayList<RecommendationModel> {

        val seenProducts = arrayListOf<ProductRecommendationModel>()
        var reformatedReponseModelList = ArrayList<RecommendationModel>()

        if(eventResponseModel != null && eventResponseModel.responses != null && eventResponseModel?.responses?.size!!>0){
        for(response in eventResponseModel.responses?.get(0)!!){

            var recommendationModel = RecommendationModel()
            recommendationModel.notificationTitle = response.params?.notificationTitle
            recommendationModel.instanceId = response.params?.instanceId
            recommendationModel.actionId = response.params?.actionId

            onImpression(recommendationModel.instanceId, recommendationModel.actionId)

            try {
                val recommendedProductsString = Gson().toJson(response.params?.recommendedProducts)
                var recommendedProductsJSON = JSONObject(recommendedProductsString)
                var recommendedProductItemArray: JSONArray? = recommendedProductsJSON.getJSONArray("RECOMMENDATION_SOURCE_STATIC_ITEMS")
                for (i in 0 until recommendedProductItemArray!!.length()) {
                    recommendationModel.products?.add(parseJsonToProductModel(recommendedProductItemArray.getJSONObject(i)))
                }
            } catch (ex: Exception) {
                SegmentifyLogger.printErrorLog(ex.message.toString())
            }
            var dynamicItemString = response.params?.dynamicItems?.replace("\"", "*")
            dynamicItemString = dynamicItemString?.replace("*", "\"")
            val listType = object : TypeToken<ArrayList<DynamicItemsModel>>() {}.type
            var dynamicItems: ArrayList<DynamicItemsModel> = Gson().fromJson<ArrayList<DynamicItemsModel>>(dynamicItemString, listType)

            for (dynamicItem in dynamicItems) {
                var recommendationProductListName: String = ""
                if (!dynamicItem.recommendationSource.isNullOrEmpty()) {
                    recommendationProductListName = dynamicItem.recommendationSource!!
                    if (!dynamicItem.timeFrame.isNullOrBlank() || !dynamicItem.timeFrame.isNullOrEmpty()) {
                        recommendationProductListName = recommendationProductListName + "|" + dynamicItem.timeFrame

                        if (!dynamicItem.score.isNullOrBlank() || !dynamicItem.score.isNullOrEmpty()) {
                            recommendationProductListName = recommendationProductListName + "|" + dynamicItem.score
                        }
                    }

                    if (!recommendationProductListName.isNullOrBlank()) {
                        val recommendedProductsString = Gson().toJson(response.params?.recommendedProducts)
                        var recommendedProductsJSON = JSONObject(recommendedProductsString)
                        var recommendedProductItemArray: JSONArray? = recommendedProductsJSON.getJSONArray(recommendationProductListName)

                        var i = 0
                        for (k in 0 until recommendedProductItemArray!!.length()) {
                            var productModel = parseJsonToProductModel(recommendedProductItemArray!!.getJSONObject(k))
                            if (!isProductSeen(seenProducts, productModel)) {
                                recommendationModel.products?.add(productModel)
                                i = i + 1
                            } else {
                                if (i > 0) {
                                    i = i - 1
                                }
                            }

                            if (i == dynamicItem.itemCount!!) {
                                break
                            }
                        }
                    }
                }
            }

            reformatedReponseModelList.add(recommendationModel)
        }

        }

        return reformatedReponseModelList
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
