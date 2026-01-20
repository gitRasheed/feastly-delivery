package com.example.feastly.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.example.feastly.events.OrderAcceptedEvent
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test to verify order-service publishes events in the expected format
 * that can be consumed by downstream services (e.g. dispatch-service).
 */
class OrderServiceContractTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `OrderAcceptedEvent produced by order-service matches consumer schema`() {
        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val timestamp = Instant.now()

        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = restaurantId,
            timestamp = timestamp
        )

        val json = objectMapper.writeValueAsString(event)
        val consumed: OrderAcceptedEvent = objectMapper.readValue(json)

        assertEquals(orderId, consumed.orderId)
        assertEquals(restaurantId, consumed.restaurantId)
        assertEquals(timestamp, consumed.timestamp)
    }

    @Test
    fun `OrderAcceptedEvent JSON can be parsed by consumer with expected fields`() {
        val orderId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val restaurantId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

        val producerJson = """
            {
                "orderId": "$orderId",
                "restaurantId": "$restaurantId",
                "timestamp": "2025-01-15T10:30:00Z"
            }
        """.trimIndent()

        val event: OrderAcceptedEvent = objectMapper.readValue(producerJson)

        assertEquals(orderId, event.orderId)
        assertEquals(restaurantId, event.restaurantId)
        assertEquals(Instant.parse("2025-01-15T10:30:00Z"), event.timestamp)
    }
}
