package com.example.feastly.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents a line item within an order.
 *
 * ## Snapshot Strategy
 * This entity stores an immutable snapshot of menu item data at order creation time:
 * - [menuItemId]: Reference to the original menu item (for traceability)
 * - [menuItemName]: Snapshot of the item name at order time (for read independence)
 * - [priceCents]: Snapshot of the price at order time (for billing accuracy)
 *
 * This design ensures order reads are **independent of restaurant-service availability**.
 * The snapshot is created at write-time and never updated.
 */
@Entity
@Table(name = "order_items")
class OrderItem(
    @Id val id: UUID = UUID.randomUUID(),

    @Column(name = "order_id", nullable = false)
    val orderId: UUID,

    @Column(name = "menu_item_id", nullable = false)
    val menuItemId: UUID,

    /**
     * Snapshot of the menu item name at order creation time.
     * This field enables order reads without calling restaurant-service.
     */
    @Column(name = "menu_item_name")
    val menuItemName: String? = null,

    @Column(nullable = false)
    val quantity: Int,

    @Column(name = "price_cents", nullable = false)
    val priceCents: Int
)
