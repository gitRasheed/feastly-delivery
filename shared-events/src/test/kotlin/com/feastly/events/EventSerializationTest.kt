package com.feastly.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract tests to verify all domain events can be serialized and deserialized
 * correctly via Jackson. This ensures cross-service compatibility.
 */
class EventSerializationTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `OrderPlacedEvent roundtrip serialization`() {
        val event = OrderPlacedEvent(
            orderId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            totalCents = 2500,
            timestamp = Instant.parse("2025-01-01T12:00:00Z")
        )

        val json = objectMapper.writeValueAsString(event)
        val deserialized: OrderPlacedEvent = objectMapper.readValue(json)

        assertEquals(event, deserialized)
    }

    @Test
    fun `OrderAcceptedEvent roundtrip serialization`() {
        val event = OrderAcceptedEvent(
            orderId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID(),
            timestamp = Instant.parse("2025-01-01T12:00:00Z")
        )

        val json = objectMapper.writeValueAsString(event)
        val deserialized: OrderAcceptedEvent = objectMapper.readValue(json)

        assertEquals(event, deserialized)
    }

    @Test
    fun `RestaurantCreatedEvent roundtrip serialization`() {
        val event = RestaurantCreatedEvent(
            restaurantId = UUID.randomUUID(),
            name = "Test Restaurant",
            timestamp = Instant.parse("2025-01-01T12:00:00Z")
        )

        val json = objectMapper.writeValueAsString(event)
        val deserialized: RestaurantCreatedEvent = objectMapper.readValue(json)

        assertEquals(event, deserialized)
    }

    @Test
    fun `DriverLocationEvent roundtrip serialization`() {
        val event = DriverLocationEvent(
            driverId = UUID.randomUUID(),
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = Instant.parse("2025-01-01T12:00:00Z")
        )

        val json = objectMapper.writeValueAsString(event)
        val deserialized: DriverLocationEvent = objectMapper.readValue(json)

        assertEquals(event, deserialized)
    }

    @Test
    fun `OrderAcceptedEvent JSON structure matches expected schema`() {
        val orderId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val restaurantId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = restaurantId,
            timestamp = Instant.parse("2025-01-01T12:00:00Z")
        )

        val json = objectMapper.writeValueAsString(event)
        
        // Verify JSON contains expected fields
        assert(json.contains("\"orderId\":\"$orderId\""))
        assert(json.contains("\"restaurantId\":\"$restaurantId\""))
        assert(json.contains("\"timestamp\":\"2025-01-01T12:00:00Z\""))
    }
}
