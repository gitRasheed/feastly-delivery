package com.feastly.drivertracking

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.feastly.events.DriverLocationEvent
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
    fun `DriverLocationEvent serializes without type headers`() {
        val event = DriverLocationEvent(
            driverId = UUID.randomUUID(),
            latitude = 40.7128,
            longitude = -74.0060
        )

        val serializer = JsonSerializer<Any>()
        serializer.configure(mapOf(JsonSerializer.ADD_TYPE_INFO_HEADERS to false), false)
        
        val headers = RecordHeaders()
        val bytes = serializer.serialize("test-topic", headers, event)!!
        
        val typeHeader = headers.lastHeader("__TypeId__")
        assertNull(typeHeader, "Type header should NOT be present - breaks cross-service compatibility")

        val json = String(bytes, Charsets.UTF_8)
        assertTrue(json.contains("driverId"), "JSON should contain driverId field")
        assertTrue(json.contains("latitude"), "JSON should contain latitude field")
        assertTrue(json.contains("longitude"), "JSON should contain longitude field")
    }

    @Test
    fun `DriverLocationEvent JSON is deserializable by stranger class`() {
        val event = DriverLocationEvent(
            driverId = UUID.randomUUID(),
            latitude = 40.7128,
            longitude = -74.0060
        )

        val json = objectMapper.writeValueAsString(event)

        val stranger = objectMapper.readValue(json, StubDriverLocationEvent::class.java)
        
        assertEquals(event.driverId, stranger.driverId)
        assertEquals(event.latitude, stranger.latitude)
        assertEquals(event.longitude, stranger.longitude)
    }

    data class StubDriverLocationEvent(
        val driverId: UUID,
        val latitude: Double,
        val longitude: Double
    )
}
