package com.example.feastly.driverstatus

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DriverStatusRepository : JpaRepository<DriverStatus, UUID> {
    fun findByIsAvailableTrue(): List<DriverStatus>
}
