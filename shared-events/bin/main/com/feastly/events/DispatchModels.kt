package com.feastly.events

import java.util.UUID

/**
 * Command to initiate dispatch for an order.
 */
data class DispatchCommand(
    val orderId: UUID,
    val restaurantId: UUID? = null
)

/**
 * Result of a dispatch operation.
 */
data class DispatchResult(
    val success: Boolean,
    val message: String,
    val assignedDriverId: UUID? = null
)

/**
 * Minimal order info needed by dispatch service.
 */
data class OrderInfo(
    val orderId: UUID,
    val status: OrderStatus,
    val assignedDriverId: UUID?,
    val restaurantLat: Double,
    val restaurantLng: Double
)

/**
 * Driver availability info for dispatch matching.
 */
data class AvailableDriver(
    val driverId: UUID,
    val latitude: Double,
    val longitude: Double
)
