package com.feastly.drivertracking

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.UUID

data class DriverStatusUpdateRequest(
    val isAvailable: Boolean,
    @field:Min(-90) @field:Max(90)
    val latitude: Double,
    @field:Min(-180) @field:Max(180)
    val longitude: Double
)

data class DriverStatusResponse(
    val driverId: UUID,
    val isAvailable: Boolean,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Instant
)

fun DriverStatus.toResponse() = DriverStatusResponse(
    driverId = driverId,
    isAvailable = isAvailable,
    latitude = latitude,
    longitude = longitude,
    lastUpdated = lastUpdated
)
