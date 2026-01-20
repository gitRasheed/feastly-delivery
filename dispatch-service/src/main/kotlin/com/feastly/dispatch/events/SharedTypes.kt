package com.feastly.dispatch.events

import java.util.UUID

/**
 * Local shared types for dispatch-service, decoupled from shared-events.
 */

enum class OrderStatus {
    SUBMITTED,
    ACCEPTED,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}

enum class DispatchAttemptStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELLED
}

data class OrderInfo(
    val orderId: UUID,
    val status: OrderStatus,
    val assignedDriverId: UUID?,
    val restaurantLat: Double,
    val restaurantLng: Double
)

data class AvailableDriver(
    val driverId: UUID,
    val latitude: Double,
    val longitude: Double
)
