package com.example.feastly.order

import com.example.feastly.payment.PaymentStatus
import com.example.feastly.restaurant.Restaurant
import com.example.feastly.user.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class OrderStatus {
    SUBMITTED,
    ACCEPTED,
    PREPARING,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}

@Entity
@Table(name = "orders")
class DeliveryOrder(
    @Id val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val restaurant: Restaurant,

    @Column(name = "driver_id")
    var driverId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.SUBMITTED,

    @Column(name = "total_cents")
    var totalCents: Int? = null,

    @Column(name = "items_subtotal_cents", nullable = false)
    var itemsSubtotalCents: Int = 0,

    @Column(name = "service_fee_cents", nullable = false)
    var serviceFeeCents: Int = 0,

    @Column(name = "delivery_fee_cents", nullable = false)
    var deliveryFeeCents: Int = 0,

    @Column(name = "discount_cents", nullable = false)
    var discountCents: Int = 0,

    @Column(name = "tip_cents", nullable = false)
    var tipCents: Int = 0,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "payment_reference")
    var paymentReference: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
