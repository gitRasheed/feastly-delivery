package com.feastly.dispatch

import com.feastly.dispatch.events.DispatchAttemptStatus
import java.time.Instant
import java.util.UUID

data class DispatchOfferResponse(
    val accepted: Boolean
)

data class DispatchStatusResponse(
    val orderId: UUID,
    val currentDriverId: UUID?,
    val pendingOfferId: UUID?,
    val status: String
)

data class DispatchAttemptResponse(
    val id: UUID?,
    val orderId: UUID,
    val driverId: UUID?,
    val status: DispatchAttemptStatus,
    val offeredAt: Instant,
    val respondedAt: Instant?
)

fun DispatchAttempt.toResponse() = DispatchAttemptResponse(
    id = id,
    orderId = orderId,
    driverId = driverId,
    status = status,
    offeredAt = offeredAt,
    respondedAt = respondedAt
)

