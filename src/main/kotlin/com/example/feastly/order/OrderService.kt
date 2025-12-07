package com.example.feastly.order

import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.common.RestaurantNotFoundException
import com.example.feastly.common.UserNotFoundException
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
    private val restaurantRepository: RestaurantRepository
) {

    fun create(userId: UUID, request: CreateOrderRequest): DeliveryOrder {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val restaurant = restaurantRepository.findByIdOrNull(request.restaurantId)
            ?: throw RestaurantNotFoundException(request.restaurantId)

        val order = DeliveryOrder(
            user = user,
            restaurant = restaurant,
            driverId = request.driverId,
            status = OrderStatus.AWAITING_RESTAURANT
        )

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
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
        return orderRepository.findByUser_Id(userId)
    }

    @Transactional(readOnly = true)
    fun listByUser(userId: UUID): List<DeliveryOrder> {
        return orderRepository.findByUser_Id(userId)
    }
}
