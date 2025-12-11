package com.example.feastly.order

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<DeliveryOrder, UUID> {
    fun findByUser_Id(userId: UUID): List<DeliveryOrder>
}

