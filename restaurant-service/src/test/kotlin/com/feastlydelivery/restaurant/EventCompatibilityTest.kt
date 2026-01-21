package com.feastlydelivery.restaurant

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.feastlydelivery.restaurant.events.RestaurantOrderAcceptedEvent
import com.feastlydelivery.restaurant.events.RestaurantOrderRejectedEvent
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.serializer.JsonSerializer
import java.util.UUID

/**
 * Black-box serialization tests proving our events are decoupled "Pure JSON"
 * without Java type headers that would couple consumers to our classes.
 */
class EventCompatibilityTest {

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun `RestaurantOrderAcceptedEvent serializes without type headers`() {
        val event = RestaurantOrderAcceptedEvent(
            orderId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID()
        )

        val serializer = JsonSerializer<Any>()
        serializer.configure(mapOf(JsonSerializer.ADD_TYPE_INFO_HEADERS to false), false)
        
        val headers = RecordHeaders()
        val bytes = serializer.serialize("test-topic", headers, event)!!
        
        val typeHeader = headers.lastHeader("__TypeId__")
        assertNull(typeHeader, "Type header should NOT be present - breaks cross-service compatibility")

        val json = String(bytes, Charsets.UTF_8)
        assertTrue(json.contains("orderId"), "JSON should contain orderId field")
        assertTrue(json.contains("restaurantId"), "JSON should contain restaurantId field")
    }

    @Test
    fun `RestaurantOrderRejectedEvent serializes without type headers`() {
        val event = RestaurantOrderRejectedEvent(
            orderId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID(),
            reason = "Restaurant is closed"
        )

        val serializer = JsonSerializer<Any>()
        serializer.configure(mapOf(JsonSerializer.ADD_TYPE_INFO_HEADERS to false), false)
        
        val headers = RecordHeaders()
        val bytes = serializer.serialize("test-topic", headers, event)!!
        
        val typeHeader = headers.lastHeader("__TypeId__")
        assertNull(typeHeader, "Type header should NOT be present - breaks cross-service compatibility")

        val json = String(bytes, Charsets.UTF_8)
        assertTrue(json.contains("reason"), "JSON should contain reason field")
    }

    @Test
    fun `event JSON is deserializable by a stranger class - proves decoupling`() {
        val event = RestaurantOrderAcceptedEvent(
            orderId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID()
        )

        val json = objectMapper.writeValueAsString(event)

        val stranger = objectMapper.readValue(json, StubAcceptedEvent::class.java)
        
        assertEquals(event.orderId, stranger.orderId)
        assertEquals(event.restaurantId, stranger.restaurantId)
    }

    @Test
    fun `rejected event JSON is deserializable by a stranger class`() {
        val event = RestaurantOrderRejectedEvent(
            orderId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID(),
            reason = "Restaurant not found"
        )

        val json = objectMapper.writeValueAsString(event)

        val stranger = objectMapper.readValue(json, StubRejectedEvent::class.java)
        
        assertEquals(event.orderId, stranger.orderId)
        assertEquals(event.reason, stranger.reason)
    }

    data class StubAcceptedEvent(
        val orderId: UUID,
        val restaurantId: UUID
    )

    data class StubRejectedEvent(
        val orderId: UUID,
        val restaurantId: UUID,
        val reason: String
    )
}
