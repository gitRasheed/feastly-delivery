package com.example.feastly.order

import com.example.feastly.payment.PaymentStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class OrderItemRequest(
    @field:NotNull(message = "menuItemId is required")
    val menuItemId: UUID,

    @field:NotNull(message = "quantity is required")
    @field:Min(value = 1, message = "quantity must be at least 1")
    val quantity: Int
)

data class CreateOrderRequest(
    @field:NotNull(message = "restaurantId is required")
    val restaurantId: UUID,

    @field:NotEmpty(message = "items cannot be empty")
    @field:Valid
    val items: List<OrderItemRequest>,

    val driverId: UUID? = null,
    val discountCode: String? = null,
    val tipCents: Int? = 0
)

data class UpdateOrderStatusRequest(
    @field:NotNull val status: OrderStatus
)

data class OrderItemResponse(
    val id: UUID,
    val menuItemId: UUID,
    val menuItemName: String,
    val quantity: Int,
    val priceCents: Int,
    val lineTotalCents: Int
)

data class OrderResponse(
    val id: UUID,
    val userId: UUID,
    val restaurantId: UUID,
    val driverId: UUID?,
    val status: OrderStatus,
    val itemsSubtotalCents: Int,
    val serviceFeeCents: Int,
    val deliveryFeeCents: Int,
    val discountCents: Int,
    val tipCents: Int,
    val totalCents: Int?,
    val items: List<OrderItemResponse>,
    val paymentStatus: PaymentStatus,
    val paymentReference: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class OrderHistoryResponse(
    val orderId: UUID,
    val totalCents: Int?,
    val restaurantName: String,
    val status: OrderStatus,
    val itemCount: Int
)

fun OrderItem.toResponse() = OrderItemResponse(
    id = this.id,
    menuItemId = this.menuItem.id,
    menuItemName = this.menuItem.name,
    quantity = this.quantity,
    priceCents = this.priceCents,
    lineTotalCents = this.quantity * this.priceCents
)

fun DeliveryOrder.toResponse() = OrderResponse(
    id = this.id,
    userId = this.user.id,
    restaurantId = this.restaurant.id,
    driverId = this.driverId,
    status = this.status,
    itemsSubtotalCents = this.itemsSubtotalCents,
    serviceFeeCents = this.serviceFeeCents,
    deliveryFeeCents = this.deliveryFeeCents,
    discountCents = this.discountCents,
    tipCents = this.tipCents,
    totalCents = this.totalCents,
    items = this.items.map { it.toResponse() },
    paymentStatus = this.paymentStatus,
    paymentReference = this.paymentReference,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun DeliveryOrder.toHistoryResponse() = OrderHistoryResponse(
    orderId = this.id,
    totalCents = this.totalCents,
    restaurantName = this.restaurant.name,
    status = this.status,
    itemCount = this.items.size
)
