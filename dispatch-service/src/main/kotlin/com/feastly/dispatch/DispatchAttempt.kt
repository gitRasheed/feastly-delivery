package com.feastly.dispatch

import com.feastly.events.DispatchAttemptStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "dispatch_attempts")
class DispatchAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    val orderId: UUID,

    val driverId: UUID,

    @Enumerated(EnumType.STRING)
    var status: DispatchAttemptStatus = DispatchAttemptStatus.PENDING,

    val offeredAt: Instant = Instant.now(),

    var respondedAt: Instant? = null
)
