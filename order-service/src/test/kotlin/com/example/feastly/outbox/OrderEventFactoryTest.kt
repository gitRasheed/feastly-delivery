package com.example.feastly.outbox

import com.example.feastly.order.DeliveryOrder
import com.example.feastly.order.OrderItem
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.UUID

/**
 * Unit tests for OrderEventFactory to verify event payload shape.
 */
class OrderEventFactoryTest {

    private val factory = OrderEventFactory(null) // No tracer for unit tests

    @Test
    fun `event payload contains required envelope fields`() {
        // Given
        val order = createTestOrder()
        val items = createTestItems(order.id)

        // When
        val payload = factory.buildEventPayload(
            eventType = OrderEventType.ORDER_SUBMITTED,
            order = order,
            items = items
        )

        // Then
        val json = jacksonObjectMapper().readValue<Map<String, Any>>(payload)

        // Verify envelope fields
        assertNotNull(json["eventId"], "eventId must be present")
        assertEquals("ORDER_SUBMITTED", json["eventType"])
        assertNotNull(json["occurredAt"], "occurredAt must be present")
        assertNotNull(json["trace"], "trace must be present")
        assertNotNull(json["order"], "order must be present")
    }

    @Test
    fun `event payload contains order snapshot with items`() {
        // Given
        val order = createTestOrder()
        val items = createTestItems(order.id)

        // When
        val payload = factory.buildEventPayload(
            eventType = OrderEventType.ORDER_SUBMITTED,
            order = order,
            items = items
        )

        // Then
        val json = jacksonObjectMapper().readValue<Map<String, Any>>(payload)

        @Suppress("UNCHECKED_CAST")
        val orderSnapshot = json["order"] as Map<String, Any>

        // Verify order fields
        assertNotNull(orderSnapshot["orderId"])
        assertNotNull(orderSnapshot["customerId"])
        assertNotNull(orderSnapshot["restaurantId"])
        assertEquals("SUBMITTED", orderSnapshot["status"])
        assertNotNull(orderSnapshot["createdAt"])

        // Verify pricing
        @Suppress("UNCHECKED_CAST")
        val pricing = orderSnapshot["pricing"] as Map<String, Any>
        assertEquals(3000, pricing["subtotalCents"])
        assertEquals(300, pricing["taxCents"])
        assertEquals(299, pricing["deliveryFeeCents"])
        assertEquals(3599, pricing["totalCents"])

        // Verify items with snapshots
        @Suppress("UNCHECKED_CAST")
        val itemsList = orderSnapshot["items"] as List<Map<String, Any>>
        assertEquals(1, itemsList.size)

        val item = itemsList[0]
        assertNotNull(item["menuItemId"])
        assertEquals("Test Pizza", item["menuItemName"])
        assertEquals(2, item["quantity"])
        assertEquals(1500, item["priceCents"])
    }

    @Test
    fun `trace context handles null tracer gracefully`() {
        // Given
        val order = createTestOrder()
        val items = createTestItems(order.id)

        // When
        val payload = factory.buildEventPayload(
            eventType = OrderEventType.ORDER_ACCEPTED,
            order = order,
            items = items
        )

        // Then
        val json = jacksonObjectMapper().readValue<Map<String, Any>>(payload)

        @Suppress("UNCHECKED_CAST")
        val trace = json["trace"] as Map<String, Any?>
        // With null tracer, traceId and spanId should be null
        assertNull(trace["traceId"])
        assertNull(trace["spanId"])
    }

    private fun createTestOrder(): DeliveryOrder {
        return DeliveryOrder(
            customerId = UUID.randomUUID(),
            restaurantId = UUID.randomUUID(),
            status = "SUBMITTED"
        ).apply {
            subtotalCents = 3000
            taxCents = 300
            deliveryFeeCents = 299
            totalCents = 3599
        }
    }

    private fun createTestItems(orderId: UUID): List<OrderItem> {
        return listOf(
            OrderItem(
                orderId = orderId,
                menuItemId = UUID.randomUUID(),
                menuItemName = "Test Pizza",
                quantity = 2,
                priceCents = 1500
            )
        )
    }
}
