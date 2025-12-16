package com.example.feastly.rating

import com.example.feastly.order.DeliveryOrder
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ratings")
class Rating(
    @Id val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    val order: DeliveryOrder,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val stars: Int,

    @Column
    val comment: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
