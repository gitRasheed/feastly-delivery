package com.feastly.dispatch

import com.feastly.events.DispatchAttemptStatus
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
    val orderId: UUID,
    val driverId: UUID?,
    val status: DispatchAttemptStatus
)

fun DispatchAttempt.toResponse() = DispatchAttemptResponse(
    orderId = orderId,
    driverId = driverId,
    status = status
)

