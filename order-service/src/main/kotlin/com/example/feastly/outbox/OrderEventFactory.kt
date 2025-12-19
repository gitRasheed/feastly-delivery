package com.example.feastly.outbox

import com.example.feastly.order.DeliveryOrder
import com.example.feastly.order.OrderItem
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Factory for building structured order event payloads for the outbox.
 *
 * All events are self-contained with order snapshots, enabling downstream
 * consumers to process events without synchronous calls to other services.
 */
@Component
class OrderEventFactory(
    private val tracer: Tracer?
) {
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    /**
     * Build a complete event envelope for an order event.
     *
     * @param eventType The type of event (e.g., ORDER_SUBMITTED, ORDER_ACCEPTED)
     * @param order The order entity
     * @param items The order items with snapshot data
     * @param occurredAt When the event occurred (defaults to now)
     * @return JSON string of the event envelope
     */
    fun buildEventPayload(
        eventType: OrderEventType,
        order: DeliveryOrder,
        items: List<OrderItem>,
        occurredAt: Instant = Instant.now()
    ): String {
        val envelope = OrderEventEnvelope(
            eventId = UUID.randomUUID(),
            eventType = eventType.name,
            occurredAt = occurredAt,
            trace = buildTraceContext(),
            order = buildOrderSnapshot(order, items)
        )
        return objectMapper.writeValueAsString(envelope)
    }

    private fun buildTraceContext(): TraceContext {
        val currentSpan: Span? = tracer?.currentSpan()
        return TraceContext(
            traceId = currentSpan?.context()?.traceId(),
            spanId = currentSpan?.context()?.spanId()
        )
    }

    private fun buildOrderSnapshot(order: DeliveryOrder, items: List<OrderItem>): OrderSnapshot {
        return OrderSnapshot(
            orderId = order.id,
            customerId = order.customerId,
            restaurantId = order.restaurantId,
            driverId = order.driverId,
            status = order.status,
            pricing = PricingSnapshot(
                subtotalCents = order.subtotalCents,
                taxCents = order.taxCents,
                deliveryFeeCents = order.deliveryFeeCents,
                totalCents = order.totalCents
            ),
            items = items.map { item ->
                OrderItemSnapshot(
                    menuItemId = item.menuItemId,
                    menuItemName = item.menuItemName,
                    quantity = item.quantity,
                    priceCents = item.priceCents
                )
            },
            createdAt = order.createdAt
        )
    }
}

/**
 * Order event types emitted from order-service.
 */
enum class OrderEventType {
    ORDER_SUBMITTED,
    ORDER_ACCEPTED,
    ORDER_REJECTED,
    ORDER_DISPATCHED,
    ORDER_DELIVERED,
    ORDER_CANCELLED
}

/**
 * Complete event envelope for order events.
 * This is the root structure serialized to the outbox payload.
 */
data class OrderEventEnvelope(
    val eventId: UUID,
    val eventType: String,
    val occurredAt: Instant,
    val trace: TraceContext,
    val order: OrderSnapshot
)

/**
 * Trace context for distributed tracing correlation.
 */
data class TraceContext(
    val traceId: String?,
    val spanId: String?
)

/**
 * Immutable snapshot of order state at event time.
 */
data class OrderSnapshot(
    val orderId: UUID,
    val customerId: UUID,
    val restaurantId: UUID,
    val driverId: UUID?,
    val status: String,
    val pricing: PricingSnapshot,
    val items: List<OrderItemSnapshot>,
    val createdAt: Instant
)

/**
 * Pricing breakdown snapshot.
 */
data class PricingSnapshot(
    val subtotalCents: Int,
    val taxCents: Int,
    val deliveryFeeCents: Int,
    val totalCents: Int
)

/**
 * Snapshot of an order item with menu data.
 * Includes menuItemName for downstream consumers to display without
 * calling restaurant-service.
 */
data class OrderItemSnapshot(
    val menuItemId: UUID,
    val menuItemName: String?,
    val quantity: Int,
    val priceCents: Int
)
