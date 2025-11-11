package com.example.feastly.order

import com.example.feastly.restaurant.RestaurantRepository
import com.example.feastly.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val restaurantRepository: RestaurantRepository
) {

    fun create(request: CreateOrderRequest): DeliveryOrder {
        val user = userRepository.findById(request.userId)
            .orElseThrow { IllegalArgumentException("User not found: ${request.userId}") }

        val restaurant = restaurantRepository.findById(request.restaurantId)
            .orElseThrow { IllegalArgumentException("Restaurant not found: ${request.restaurantId}") }

        val order = DeliveryOrder(
            user = user,
            restaurant = restaurant,
            driverId = request.driverId,
            status = OrderStatus.AWAITING_RESTAURANT
        )

        return orderRepository.save(order)
    }

    fun updateStatus(orderId: UUID, newStatus: OrderStatus): DeliveryOrder {
        val order = orderRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("Order not found: $orderId") }

        order.status = newStatus
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun listByUser(userId: UUID): List<DeliveryOrder> {
        return orderRepository.findByUser_Id(userId)
    }
}
