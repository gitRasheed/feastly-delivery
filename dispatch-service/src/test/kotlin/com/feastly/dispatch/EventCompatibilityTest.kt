package com.feastly.dispatch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.feastly.dispatch.events.DriverAssignedEventDto
import com.feastly.dispatch.events.DeliveryCompletedEventDto
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
    fun `DriverAssignedEventDto serializes without type headers`() {
        val event = DriverAssignedEventDto(
            orderId = UUID.randomUUID(),
            driverId = UUID.randomUUID()
        )

        val serializer = JsonSerializer<Any>()
        serializer.configure(mapOf(JsonSerializer.ADD_TYPE_INFO_HEADERS to false), false)
        
        val headers = RecordHeaders()
        val bytes = serializer.serialize("test-topic", headers, event)!!
        
        val typeHeader = headers.lastHeader("__TypeId__")
        assertNull(typeHeader, "Type header should NOT be present - breaks cross-service compatibility")

        val json = String(bytes, Charsets.UTF_8)
        assertTrue(json.contains("orderId"), "JSON should contain orderId field")
        assertTrue(json.contains("driverId"), "JSON should contain driverId field")
    }

    @Test
    fun `DeliveryCompletedEventDto serializes without type headers`() {
        val event = DeliveryCompletedEventDto(
            orderId = UUID.randomUUID(),
            driverId = UUID.randomUUID()
        )

        val serializer = JsonSerializer<Any>()
        serializer.configure(mapOf(JsonSerializer.ADD_TYPE_INFO_HEADERS to false), false)
        
        val headers = RecordHeaders()
        val bytes = serializer.serialize("test-topic", headers, event)!!
        
        val typeHeader = headers.lastHeader("__TypeId__")
        assertNull(typeHeader, "Type header should NOT be present - breaks cross-service compatibility")

        val json = String(bytes, Charsets.UTF_8)
        assertTrue(json.contains("orderId"), "JSON should contain orderId field")
    }

    @Test
    fun `DriverAssignedEventDto JSON is deserializable by stranger class`() {
        val event = DriverAssignedEventDto(
            orderId = UUID.randomUUID(),
            driverId = UUID.randomUUID()
        )

        val json = objectMapper.writeValueAsString(event)

        val stranger = objectMapper.readValue(json, StubDriverAssignedEvent::class.java)
        
        assertEquals(event.orderId, stranger.orderId)
        assertEquals(event.driverId, stranger.driverId)
    }

    @Test
    fun `DeliveryCompletedEventDto JSON is deserializable by stranger class`() {
        val event = DeliveryCompletedEventDto(
            orderId = UUID.randomUUID(),
            driverId = UUID.randomUUID()
        )

        val json = objectMapper.writeValueAsString(event)

        val stranger = objectMapper.readValue(json, StubDeliveryCompletedEvent::class.java)
        
        assertEquals(event.orderId, stranger.orderId)
        assertEquals(event.driverId, stranger.driverId)
    }

    data class StubDriverAssignedEvent(
        val orderId: UUID,
        val driverId: UUID
    )

    data class StubDeliveryCompletedEvent(
        val orderId: UUID,
        val driverId: UUID
    )
}
