package com.example.feastly.events

import java.time.Instant
import java.util.UUID

/**
 * Local domain events for order-service.
 * These are the canonical event definitions used by this service.
 */

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

data class RestaurantOrderRequest(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant = Instant.now()
)

data class RestaurantOrderAcceptedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant = Instant.now()
)

data class AssignDriverCommand(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant = Instant.now()
)

data class DriverAssignedEvent(
    val orderId: UUID,
    val driverId: UUID,
    val timestamp: Instant = Instant.now()
)

data class DeliveryCompletedEvent(
    val orderId: UUID,
    val driverId: UUID,
    val timestamp: Instant = Instant.now()
)

data class DriverDeliveryFailedEvent(
    val orderId: UUID,
    val driverId: UUID,
    val reason: String,
    val timestamp: Instant = Instant.now()
)
