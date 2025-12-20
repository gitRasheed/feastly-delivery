package com.example.feastly.client

import java.time.Instant
import java.util.UUID

interface RestaurantAvailabilityClient {
    fun checkAvailability(restaurantId: UUID, at: Instant = Instant.now()): RestaurantAvailabilityResponse
}

data class RestaurantAvailabilityResponse(
    val accepting: Boolean,
    val reason: String,
    val nextChangeAt: Instant?
)
