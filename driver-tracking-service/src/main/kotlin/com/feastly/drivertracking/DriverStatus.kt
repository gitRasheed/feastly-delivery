package com.feastly.drivertracking

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "driver_status")
class DriverStatus(
    @Id
    val driverId: UUID,

    var isAvailable: Boolean = false,

    var latitude: Double = 0.0,

    var longitude: Double = 0.0,

    var lastUpdated: Instant = Instant.now()
)
