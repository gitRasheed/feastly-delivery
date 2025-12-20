package com.feastly.drivertracking

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DriverStatusRepository : JpaRepository<DriverStatus, UUID> {
    fun findByIsAvailableTrue(): List<DriverStatus>
}
