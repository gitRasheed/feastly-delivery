package com.feastly.events

import java.time.Instant
import java.util.UUID

/**
 * Event published when a driver's location is updated.
 */
data class DriverLocationEvent(
    val driverId: UUID,
    val latitude: Double,
    val longitude: Double,
    override val timestamp: Instant = Instant.now()
) : DomainEvent
