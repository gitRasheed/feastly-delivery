package com.example.feastly.events

import java.time.Instant
import java.util.UUID

data class OrderPlacedEvent(
    val orderId: UUID,
    val userId: UUID,
    val totalCents: Int,
    val timestamp: Instant = Instant.now()
)

data class OrderAcceptedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant = Instant.now()
)

data class RestaurantCreatedEvent(
    val restaurantId: UUID,
    val name: String,
    val timestamp: Instant = Instant.now()
)
