package com.feastly.drivertracking

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/drivers")
class DriverStatusController(
    private val driverStatusService: DriverStatusService,
    private val driverStatusRepository: DriverStatusRepository
) {

    @GetMapping("/driver-status")
    fun getAllDrivers(): ResponseEntity<List<DriverStatusResponse>> {
        val drivers = driverStatusRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(drivers)
    }

    @PutMapping("/{driverId}/status")
    fun updateStatus(
        @PathVariable driverId: UUID,
        @Valid @RequestBody request: DriverStatusUpdateRequest
    ): ResponseEntity<DriverStatusResponse> {
        val response = driverStatusService.updateStatus(driverId, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{driverId}/status")
    fun getStatus(@PathVariable driverId: UUID): ResponseEntity<DriverStatusResponse> {
        val response = driverStatusService.getStatus(driverId)
        return if (response != null) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/available")
    fun getAvailableDrivers(): ResponseEntity<List<DriverStatusResponse>> {
        val drivers = driverStatusService.getAvailableDrivers().map { it.toResponse() }
        return ResponseEntity.ok(drivers)
    }
}

