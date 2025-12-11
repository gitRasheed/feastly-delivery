package com.example.feastly.order

import com.example.feastly.common.MenuItemNotFoundException
import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.common.RestaurantNotFoundException
import com.example.feastly.common.UserNotFoundException
import com.example.feastly.menu.MenuItemRepository
import com.example.feastly.restaurant.RestaurantRepository
import com.example.feastly.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val restaurantRepository: RestaurantRepository,
    private val menuItemRepository: MenuItemRepository
) {

    fun create(userId: UUID, request: CreateOrderRequest): DeliveryOrder {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val restaurant = restaurantRepository.findByIdOrNull(request.restaurantId)
            ?: throw RestaurantNotFoundException(request.restaurantId)

        // Fetch and validate all menu items belong to the restaurant
        val menuItems = request.items.map { itemRequest ->
            val menuItem = menuItemRepository.findByIdOrNull(itemRequest.menuItemId)
                ?: throw MenuItemNotFoundException(itemRequest.menuItemId)

            require(menuItem.restaurant.id == request.restaurantId) {
                "Menu item ${menuItem.id} does not belong to restaurant ${request.restaurantId}"
            }

            menuItem to itemRequest.quantity
        }

        // Calculate total
        val totalCents = menuItems.sumOf { (menuItem, quantity) ->
            menuItem.priceCents * quantity
        }

        // Create order
        val order = DeliveryOrder(
            user = user,
            restaurant = restaurant,
            driverId = request.driverId,
            status = OrderStatus.AWAITING_RESTAURANT,
            totalCents = totalCents
        )

        // Create order items and add to order (cascade will persist them)
        val orderItems = menuItems.map { (menuItem, quantity) ->
            OrderItem(
                order = order,
                menuItem = menuItem,
                quantity = quantity,
                priceCents = menuItem.priceCents
            )
        }
        order.items.addAll(orderItems)

        // Save order - cascade will persist order items
        return orderRepository.save(order)
    }

    fun updateStatus(orderId: UUID, newStatus: OrderStatus): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        order.status = newStatus
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getOrdersForUser(userId: UUID): List<DeliveryOrder> {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
        return orderRepository.findByUser_Id(userId)
    }
}

