package com.example.feastly.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class OrderStatus {
    SUBMITTED,
    ACCEPTED,
    PREPARING,
    AWAITING_DRIVER,
    DRIVER_ASSIGNED,
    DISPATCHED,
    DELIVERED,
    CANCELLED,
    DISPATCH_FAILED
}

@Entity
@Table(name = "orders")
class DeliveryOrder(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(name = "driver_id")
    var driverId: UUID? = null,

    @Column(nullable = false)
    var status: String = OrderStatus.SUBMITTED.name,

    @Column(name = "subtotal_cents", nullable = false)
    var subtotalCents: Int = 0,

    @Column(name = "tax_cents", nullable = false)
    var taxCents: Int = 0,

    @Column(name = "delivery_fee_cents", nullable = false)
    var deliveryFeeCents: Int = 0,

    @Column(name = "total_cents", nullable = false)
    var totalCents: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun getOrderStatus(): OrderStatus = OrderStatus.valueOf(status)
    fun setOrderStatus(orderStatus: OrderStatus) { status = orderStatus.name }
}
