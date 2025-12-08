package com.example.feastly.rating

import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.common.UserNotFoundException
import com.example.feastly.order.OrderRepository
import com.example.feastly.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RatingService(
    private val ratingRepository: RatingRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun rateOrder(userId: UUID, orderId: UUID, request: RatingRequest): Rating {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        require(order.user.id == userId) { "Order does not belong to this user" }

        require(!ratingRepository.existsByOrderId(orderId)) { "Order has already been rated" }

        val rating = Rating(
            order = order,
            user = user,
            stars = request.stars,
            comment = request.comment
        )

        return ratingRepository.save(rating)
    }
}
