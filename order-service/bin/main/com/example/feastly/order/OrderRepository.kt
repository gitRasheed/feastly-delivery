package com.example.feastly.order

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<DeliveryOrder, UUID> {
    fun findByCustomerId(customerId: UUID): List<DeliveryOrder>
    fun findByStatus(status: String): List<DeliveryOrder>
}
