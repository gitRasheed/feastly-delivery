package com.example.feastly.dispatch

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class DispatchController(
    private val dispatchService: DispatchService
) {

    @PostMapping("/orders/{orderId}/dispatch")
    fun startDispatch(@PathVariable orderId: UUID): ResponseEntity<DispatchStatusResponse> {
        dispatchService.startDispatch(orderId)
        val status = dispatchService.getDispatchStatus(orderId)
        return ResponseEntity.ok(status)
    }

    @PostMapping("/orders/{orderId}/offer-response")
    fun respondToOffer(
        @PathVariable orderId: UUID,
        @RequestParam driverId: UUID,
        @RequestBody request: DispatchOfferResponse
    ): ResponseEntity<Void> {
        val success = dispatchService.handleOfferResponse(orderId, driverId, request.accepted)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/orders/{orderId}/dispatch-status")
    fun getDispatchStatus(@PathVariable orderId: UUID): ResponseEntity<DispatchStatusResponse> {
        val status = dispatchService.getDispatchStatus(orderId)
        return ResponseEntity.ok(status)
    }

    @PostMapping("/orders/{orderId}/expire-offers")
    fun expireOffers(@PathVariable orderId: UUID): ResponseEntity<Void> {
        dispatchService.expireStaleOffers(orderId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/orders/{orderId}/driver-cancel")
    fun cancelAssignment(
        @PathVariable orderId: UUID,
        @RequestParam driverId: UUID
    ): ResponseEntity<Void> {
        val success = dispatchService.driverCancelAssignment(orderId, driverId)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/dispatch/expire-pending")
    fun expireAllPending(): ResponseEntity<Map<String, Int>> {
        val count = dispatchService.expirePendingOffers()
        return ResponseEntity.ok(mapOf("expiredCount" to count))
    }
}
