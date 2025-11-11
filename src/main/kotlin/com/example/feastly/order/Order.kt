package com.example.feastly.order

import com.example.feastly.restaurant.Restaurant
import com.example.feastly.user.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

enum class OrderStatus {
    AWAITING_RESTAURANT,
    PENDING,
    OUT_FOR_DELIVERY,
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
    val driverId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "order_status")
    var status: OrderStatus = OrderStatus.AWAITING_RESTAURANT,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
