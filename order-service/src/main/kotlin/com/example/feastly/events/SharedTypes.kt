package com.example.feastly.events

import java.util.UUID

/**
 * Shared types for order-service, local copies for decoupling.
 */

enum class OrderStatus {
    SUBMITTED,
    ACCEPTED,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}

/**
 * Minimal order info for internal API responses.
 */
data class OrderInfo(
    val orderId: UUID,
    val status: OrderStatus,
    val assignedDriverId: UUID?,
    val restaurantLat: Double,
    val restaurantLng: Double
)
