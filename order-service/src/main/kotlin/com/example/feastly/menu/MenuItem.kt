package com.example.feastly.menu

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "menu_items")
class MenuItem(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column
    val description: String? = null,

    @Column(name = "price_cents", nullable = false)
    val priceCents: Int,

    @Column(nullable = false)
    var available: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
