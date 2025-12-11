package com.example.feastly.driverstatus

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
    private val service: DriverStatusService
) {

    @PutMapping("/{driverId}/status")
    fun updateStatus(
        @PathVariable driverId: UUID,
        @Valid @RequestBody request: DriverStatusUpdateRequest
    ): ResponseEntity<DriverStatusResponse> {
        val response = service.updateStatus(driverId, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{driverId}/status")
    fun getStatus(@PathVariable driverId: UUID): ResponseEntity<DriverStatusResponse> {
        val response = service.getStatus(driverId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(response)
    }

    @GetMapping("/available")
    fun getAvailableDrivers(): List<DriverStatusResponse> {
        return service.getAvailableDrivers().map { it.toResponse() }
    }
}
