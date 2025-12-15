package com.example.feastly.rating

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RatingRepository : JpaRepository<Rating, UUID> {
    fun existsByOrderId(orderId: UUID): Boolean
}
