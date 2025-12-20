package com.feastlydelivery.restaurant

import java.time.Instant

enum class AvailabilityReason {
    OPEN,
    OFFLINE,
    OUTSIDE_SCHEDULE,
    FORCED_OPEN,
    FORCED_CLOSED
}

data class RestaurantAvailabilityResponse(
    val accepting: Boolean,
    val reason: AvailabilityReason,
    val nextChangeAt: Instant?
)
