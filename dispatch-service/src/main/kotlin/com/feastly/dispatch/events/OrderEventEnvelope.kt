package com.feastly.dispatch.events

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant
import java.util.UUID

/**
 * DTOs for parsing order event envelopes from order-service.
 * These mirror the envelope structure from order-service's OrderEventFactory.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderEventEnvelope(
    val eventId: UUID,
    val eventType: String,
    val occurredAt: Instant? = null,
    val trace: TraceContext? = null,
    val order: OrderSnapshot
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TraceContext(
    val traceId: String? = null,
    val spanId: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderSnapshot(
    val orderId: UUID,
    val customerId: UUID,
    val restaurantId: UUID,
    val driverId: UUID? = null,
    val status: String,
    val pricing: PricingSnapshot,
    val items: List<OrderItemSnapshot> = emptyList(),
    val createdAt: Instant? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PricingSnapshot(
    val subtotalCents: Int = 0,
    val taxCents: Int = 0,
    val deliveryFeeCents: Int = 0,
    val totalCents: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderItemSnapshot(
    val menuItemId: UUID,
    val menuItemName: String? = null,
    val quantity: Int,
    val priceCents: Int
)

/**
 * Event types emitted by order-service.
 */
object OrderEventTypes {
    const val ORDER_SUBMITTED = "ORDER_SUBMITTED"
    const val ORDER_ACCEPTED = "ORDER_ACCEPTED"
    const val ORDER_REJECTED = "ORDER_REJECTED"
    const val ORDER_DISPATCHED = "ORDER_DISPATCHED"
    const val ORDER_DELIVERED = "ORDER_DELIVERED"
    const val ORDER_CANCELLED = "ORDER_CANCELLED"
}
