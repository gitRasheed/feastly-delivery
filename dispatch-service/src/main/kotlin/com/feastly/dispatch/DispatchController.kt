package com.feastly.dispatch

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/dispatch")
class DispatchController(
    private val dispatchService: DispatchService,
    private val dispatchAttemptRepository: DispatchAttemptRepository
) {

    @GetMapping("/attempts")
    fun getAllAttempts(): ResponseEntity<List<DispatchAttemptResponse>> {
        val attempts = dispatchAttemptRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(attempts)
    }

    @PostMapping("/orders/{orderId}/start")
    fun startDispatch(@PathVariable orderId: UUID): ResponseEntity<DispatchAttemptResponse> {
        val attempt = dispatchService.startDispatch(orderId)
        return if (attempt != null) {
            ResponseEntity.ok(attempt.toResponse())
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @PostMapping("/orders/{orderId}/drivers/{driverId}/respond")
    fun respondToOffer(
        @PathVariable orderId: UUID,
        @PathVariable driverId: UUID,
        @RequestBody response: DispatchOfferResponse
    ): ResponseEntity<Void> {
        val success = dispatchService.handleOfferResponse(orderId, driverId, response.accepted)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/orders/{orderId}/drivers/{driverId}/cancel")
    fun cancelAssignment(
        @PathVariable orderId: UUID,
        @PathVariable driverId: UUID
    ): ResponseEntity<Void> {
        val success = dispatchService.driverCancelAssignment(orderId, driverId)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/orders/{orderId}/status")
    fun getDispatchStatus(@PathVariable orderId: UUID): ResponseEntity<DispatchStatusResponse> {
        return ResponseEntity.ok(dispatchService.getDispatchStatus(orderId))
    }

    @PostMapping("/expire-pending")
    fun expirePendingOffers(): ResponseEntity<Map<String, Int>> {
        val count = dispatchService.expirePendingOffers()
        return ResponseEntity.ok(mapOf("expiredCount" to count))
    }
}

