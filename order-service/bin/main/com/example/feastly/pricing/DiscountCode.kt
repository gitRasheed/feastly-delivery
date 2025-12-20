package com.example.feastly.pricing

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "discount_codes")
class DiscountCode(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val code: String,

    @Column
    val percentage: Int? = null,

    @Column(name = "amount_cents")
    val amountCents: Int? = null,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
