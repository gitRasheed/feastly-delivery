package com.feastlydelivery.restaurant.events

import java.time.Instant
import java.util.UUID

data class RestaurantOrderRequestDto(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant = Instant.now()
)

data class RestaurantOrderAcceptedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant = Instant.now()
)

data class RestaurantOrderRejectedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    val reason: String,
    val timestamp: Instant = Instant.now()
)
