package com.example.feastly.pricing

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "discount_codes")
class DiscountCode(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val code: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: DiscountType,

    @Column(name = "percent_bps")
    val percentBps: Int? = null,

    @Column(name = "fixed_cents")
    val fixedCents: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val scope: DiscountScope = DiscountScope.ORDER_ITEMS_ONLY,

    @Column(name = "min_items_subtotal_cents")
    val minItemsSubtotalCents: Int? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
