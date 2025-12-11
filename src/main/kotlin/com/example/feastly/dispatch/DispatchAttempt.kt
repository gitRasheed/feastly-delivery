package com.example.feastly.dispatch

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class DispatchAttemptStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}

@Entity
@Table(name = "dispatch_attempt")
class DispatchAttempt(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "order_id", nullable = false)
    val orderId: UUID,

    @Column(name = "driver_id", nullable = false)
    val driverId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DispatchAttemptStatus = DispatchAttemptStatus.PENDING,

    @Column(name = "offered_at", nullable = false)
    val offeredAt: Instant = Instant.now(),

    @Column(name = "responded_at")
    var respondedAt: Instant? = null
)
