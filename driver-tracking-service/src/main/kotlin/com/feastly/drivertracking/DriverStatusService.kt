package com.feastly.drivertracking

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class DriverStatusService(
    private val driverStatusRepository: DriverStatusRepository,
    private val driverLocationAggregator: DriverLocationAggregator
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

        val saved = driverStatusRepository.save(status)

        driverLocationAggregator.bufferUpdate(driverId, saved.latitude, saved.longitude)

        return saved.toResponse()
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
