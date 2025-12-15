package com.feastly.events

import java.time.Instant
import java.util.UUID

/**
 * Published when a new order is placed.
 */
data class OrderPlacedEvent(
    val orderId: UUID,
    val userId: UUID,
    val totalCents: Int,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Published when a restaurant accepts an order.
 */
data class OrderAcceptedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Published when a new restaurant is created.
 */
data class RestaurantCreatedEvent(
    val restaurantId: UUID,
    val name: String,
    override val timestamp: Instant = Instant.now()
) : DomainEvent
