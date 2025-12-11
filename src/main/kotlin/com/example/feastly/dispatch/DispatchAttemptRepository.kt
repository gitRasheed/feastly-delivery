package com.example.feastly.dispatch

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DispatchAttemptRepository : JpaRepository<DispatchAttempt, UUID> {
    fun findByOrderIdAndDriverId(orderId: UUID, driverId: UUID): DispatchAttempt?
    
    fun findByOrderIdAndStatus(orderId: UUID, status: DispatchAttemptStatus): List<DispatchAttempt>
    
    fun findByOrderId(orderId: UUID): List<DispatchAttempt>
    
    fun existsByOrderIdAndDriverIdAndStatusIn(
        orderId: UUID,
        driverId: UUID,
        statuses: List<DispatchAttemptStatus>
    ): Boolean
}
