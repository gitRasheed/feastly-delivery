package com.example.feastly.order

import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

// Requests

data class CreateOrderRequest(
    @field:NotNull val userId: UUID,
    @field:NotNull val restaurantId: UUID,
    val driverId: UUID? = null,
)

data class UpdateOrderStatusRequest(
    @field:NotNull val status: OrderStatus
)

// Response

data class OrderResponse(
    val id: UUID,
    val userId: UUID,
    val restaurantId: UUID,
    val driverId: UUID?,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DeliveryOrder.toResponse() = OrderResponse(
    id = this.id,
    userId = this.user.id,
    restaurantId = this.restaurant.id,
    driverId = this.driverId,
    status = this.status,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)
