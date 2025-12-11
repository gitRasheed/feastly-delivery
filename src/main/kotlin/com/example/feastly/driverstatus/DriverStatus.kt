package com.example.feastly.driverstatus

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "driver_status")
class DriverStatus(
    @Id
    @Column(name = "driver_id")
    val driverId: UUID,

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = false,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(name = "last_updated", nullable = false)
    var lastUpdated: Instant = Instant.now()
)
