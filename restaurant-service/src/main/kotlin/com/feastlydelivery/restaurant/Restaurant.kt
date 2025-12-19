package com.feastlydelivery.restaurant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_user_id", nullable = false)
    val ownerUserId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(name = "is_open", nullable = false)
    var isOpen: Boolean = false,

    @Column(name = "opens_at")
    val opensAt: LocalTime? = null,

    @Column(name = "closes_at")
    val closesAt: LocalTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "menu_categories")
class MenuCategory(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "menu_items")
class MenuItem(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(name = "category_id")
    val categoryId: UUID? = null,

    @Column(nullable = false)
    val name: String,

    val description: String? = null,

    @Column(name = "price_cents", nullable = false)
    val priceCents: Int,

    @Column(nullable = false)
    var available: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
