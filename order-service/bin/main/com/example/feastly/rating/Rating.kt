package com.example.feastly.rating

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ratings")
class Rating(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "order_id", nullable = false)
    val orderId: UUID,

    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,

    @Column(nullable = false)
    val rating: Int,

    @Column
    val comment: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
