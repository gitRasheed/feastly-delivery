package com.feastlydelivery.restaurant

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RestaurantRepository : JpaRepository<Restaurant, UUID> {
    fun findByOwnerUserId(ownerUserId: UUID): List<Restaurant>
}

interface MenuCategoryRepository : JpaRepository<MenuCategory, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<MenuCategory>
}

interface MenuItemRepository : JpaRepository<MenuItem, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<MenuItem>
    fun findByCategoryId(categoryId: UUID): List<MenuItem>
}

interface RestaurantOrderRepository : JpaRepository<RestaurantOrder, UUID>
