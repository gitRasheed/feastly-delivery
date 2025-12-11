package com.example.feastly.driverstatus

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class DriverStatusService(
    private val driverStatusRepository: DriverStatusRepository
) {

    fun updateStatus(driverId: UUID, request: DriverStatusUpdateRequest): DriverStatusResponse {
        val status = driverStatusRepository.findByIdOrNull(driverId)
            ?: DriverStatus(
                driverId = driverId,
                isAvailable = request.isAvailable,
                latitude = request.latitude,
                longitude = request.longitude
            )

        status.isAvailable = request.isAvailable
        status.latitude = request.latitude
        status.longitude = request.longitude
        status.lastUpdated = Instant.now()

        return driverStatusRepository.save(status).toResponse()
    }

    @Transactional(readOnly = true)
    fun getAvailableDrivers(): List<DriverStatus> {
        return driverStatusRepository.findByIsAvailableTrue()
    }

    @Transactional(readOnly = true)
    fun getStatus(driverId: UUID): DriverStatusResponse? {
        return driverStatusRepository.findByIdOrNull(driverId)?.toResponse()
    }
}
