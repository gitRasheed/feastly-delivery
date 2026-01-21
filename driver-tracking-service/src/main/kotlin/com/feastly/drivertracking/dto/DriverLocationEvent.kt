package com.feastly.drivertracking.dto

import java.time.Instant
import java.util.UUID

/**
 * Event published when a driver's location is updated.
 * Local DTO - no shared library coupling.
 */
data class DriverLocationEvent(
    val driverId: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant = Instant.now()
)
