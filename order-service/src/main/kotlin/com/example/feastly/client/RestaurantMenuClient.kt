package com.example.feastly.client

import java.util.UUID

/**
 * Client interface for fetching menu items from restaurant-service.
 *
 * ## Usage Context
 *
 * This client is used **only at write-time** (order creation) to:
 * 1. Validate that menu items exist and are available
 * 2. Verify items belong to the requested restaurant
 * 3. Snapshot menu item data (name, price) into order items
 *
 * ## Read-Time Independence
 *
 * This client is **never called on read paths**. Order retrieval endpoints
 * return snapshotted data from [OrderItem] entities, ensuring orders remain
 * readable even when restaurant-service is unavailable.
 *
 * @see OrderItem for snapshot storage
 * @see OrderService for the write-time usage
 */
interface RestaurantMenuClient {
    /**
     * Batch-fetch menu items by their IDs.
     *
     * Called only during order creation to validate and snapshot menu data.
     *
     * @param ids List of menu item UUIDs to fetch
     * @return List of menu item data (may be smaller than input if some IDs not found)
     */
    fun batchGetMenuItems(ids: List<UUID>): List<MenuItemData>
}

/**
 * Menu item data returned from restaurant-service.
 *
 * This data is used at order creation time to:
 * - Validate item availability
 * - Snapshot [name] and [priceCents] into order items
 */
data class MenuItemData(
    val id: UUID,
    val restaurantId: UUID,
    val priceCents: Int,
    val available: Boolean,
    val name: String
)
