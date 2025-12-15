package com.example.feastly.events

import java.time.Instant
import java.util.UUID

data class DriverLocationEvent(
    val driverId: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant = Instant.now()
)
