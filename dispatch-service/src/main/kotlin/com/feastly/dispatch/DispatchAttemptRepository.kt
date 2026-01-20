package com.feastly.dispatch

import com.feastly.dispatch.events.DispatchAttemptStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface DispatchAttemptRepository : JpaRepository<DispatchAttempt, UUID> {

    fun findByOrderId(orderId: UUID): List<DispatchAttempt>

    fun findByOrderIdAndDriverId(orderId: UUID, driverId: UUID): DispatchAttempt?

    fun findByOrderIdAndStatus(orderId: UUID, status: DispatchAttemptStatus): List<DispatchAttempt>

    fun findByStatusAndOfferedAtBefore(status: DispatchAttemptStatus, cutoff: Instant): List<DispatchAttempt>
}
