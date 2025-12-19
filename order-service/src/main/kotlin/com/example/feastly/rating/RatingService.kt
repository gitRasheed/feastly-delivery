package com.example.feastly.rating

import com.example.feastly.client.UserClient
import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.common.UserNotFoundException
import com.example.feastly.order.OrderRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RatingService(
    private val ratingRepository: RatingRepository,
    private val orderRepository: OrderRepository,
    private val userClient: UserClient
) {
    @Transactional
    fun rateOrder(userId: UUID, orderId: UUID, request: RatingRequest): Rating {
        if (!userClient.existsById(userId)) {
            throw UserNotFoundException(userId)
        }

        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        require(order.customerId == userId) { "Order does not belong to this user" }

        require(!ratingRepository.existsByOrderId(orderId)) { "Order has already been rated" }

        val rating = Rating(
            orderId = orderId,
            customerId = userId,
            rating = request.stars,
            comment = request.comment
        )

        return ratingRepository.save(rating)
    }
}
