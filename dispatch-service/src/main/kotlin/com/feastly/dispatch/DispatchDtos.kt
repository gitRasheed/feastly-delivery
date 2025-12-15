package com.feastly.dispatch

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
