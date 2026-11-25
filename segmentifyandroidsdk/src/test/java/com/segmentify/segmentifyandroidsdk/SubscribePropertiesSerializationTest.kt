package com.segmentify.segmentifyandroidsdk

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.segmentify.segmentifyandroidsdk.model.Purpose
import com.segmentify.segmentifyandroidsdk.model.SubscribeProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscribePropertiesSerializationTest {

    private val gson = Gson()

    private fun toJson(properties: SubscribeProperties): JsonObject =
        JsonParser.parseString(gson.toJson(properties)).asJsonObject

    @Test
    fun appPushSerializesChannelAndToken() {
        val json = toJson(SubscribeProperties.AppPush(fcmToken = "fcm-abc"))

        assertEquals("app_push", json.get("channel").asString)
        assertEquals("fcm-abc", json.get("fcmToken").asString)
    }

    @Test
    fun emailSerializesChannelAndPurposeList() {
        val json = toJson(
            SubscribeProperties.Email(
                email = "user@example.com",
                purpose = listOf(Purpose.MARKETING, Purpose.TRANSACTIONAL)
            )
        )

        assertEquals("email", json.get("channel").asString)
        assertEquals("user@example.com", json.get("email").asString)
        val purposes = json.getAsJsonArray("purpose")
        assertEquals(2, purposes.size())
        assertEquals("MARKETING", purposes.get(0).asString)
        assertEquals("TRANSACTIONAL", purposes.get(1).asString)
    }

    @Test
    fun whatsappSerializesChannelAndPhone() {
        val json = toJson(SubscribeProperties.Whatsapp(phone = "1234567890", purpose = listOf(Purpose.MARKETING)))

        assertEquals("whatsapp", json.get("channel").asString)
        assertEquals("1234567890", json.get("phone").asString)
    }

    @Test
    fun smsSerializesChannel() {
        val json = toJson(SubscribeProperties.Sms(phone = "1234567890", purpose = listOf(Purpose.MARKETING)))

        assertEquals("sms", json.get("channel").asString)
        assertEquals("1234567890", json.get("phone").asString)
    }

    @Test
    fun callSerializesChannel() {
        val json = toJson(SubscribeProperties.Call(phone = "1234567890", purpose = listOf(Purpose.MARKETING)))

        assertEquals("call", json.get("channel").asString)
        assertTrue(json.has("phone"))
    }
}
