package com.example.feastly.menu

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MenuItemRepository : JpaRepository<MenuItem, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<MenuItem>
    fun findByIdAndRestaurantId(id: UUID, restaurantId: UUID): MenuItem?
}
