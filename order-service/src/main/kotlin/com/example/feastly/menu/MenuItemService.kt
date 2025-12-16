package com.example.feastly.menu

import com.example.feastly.client.RestaurantClient
import com.example.feastly.common.MenuItemNotFoundException
import com.example.feastly.common.RestaurantNotFoundException
import com.example.feastly.common.UnauthorizedRestaurantAccessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class MenuItemService(
    private val menuItemRepository: MenuItemRepository,
    private val restaurantClient: RestaurantClient
) {

    fun addMenuItem(restaurantId: UUID, request: MenuItemRequest): MenuItem {
        if (!restaurantClient.existsById(restaurantId)) {
            throw RestaurantNotFoundException(restaurantId)
        }

        val menuItem = MenuItem(
            restaurantId = restaurantId,
            name = request.name,
            description = request.description,
            priceCents = request.priceCents,
            available = request.available
        )

        return menuItemRepository.save(menuItem)
    }

    fun updateMenuItem(restaurantId: UUID, menuItemId: UUID, request: MenuItemRequest): MenuItem {
        val menuItem = menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
            ?: throw MenuItemNotFoundException(menuItemId)

        val updated = MenuItem(
            id = menuItem.id,
            restaurantId = menuItem.restaurantId,
            name = request.name,
            description = request.description,
            priceCents = request.priceCents,
            available = request.available,
            createdAt = menuItem.createdAt
        )

        return menuItemRepository.save(updated)
    }

    fun deleteMenuItem(restaurantId: UUID, menuItemId: UUID) {
        val menuItem = menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
            ?: throw MenuItemNotFoundException(menuItemId)

        menuItemRepository.delete(menuItem)
    }

    fun updateAvailability(restaurantId: UUID, itemId: UUID, available: Boolean): MenuItem {
        val menuItem = menuItemRepository.findByIdOrNull(itemId)
            ?: throw MenuItemNotFoundException(itemId)

        if (menuItem.restaurantId != restaurantId) {
            throw UnauthorizedRestaurantAccessException(restaurantId)
        }

        menuItem.available = available
        return menuItemRepository.save(menuItem)
    }

    @Transactional(readOnly = true)
    fun getMenuByRestaurant(restaurantId: UUID): List<MenuItem> {
        if (!restaurantClient.existsById(restaurantId)) {
            throw RestaurantNotFoundException(restaurantId)
        }
        return menuItemRepository.findByRestaurantId(restaurantId)
    }
}
