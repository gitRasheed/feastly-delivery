package com.example.feastly.order

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class OrderController(
    private val service: OrderService
) {
    companion object {
        const val USER_ID_HEADER = "X-USER-ID"
    }

    @GetMapping("/orders")
    fun getMyOrders(@RequestHeader(USER_ID_HEADER) userId: UUID): List<OrderResponse> =
        service.getOrdersForUser(userId).map { it.toResponse() }

    @PostMapping("/orders")
    fun create(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val saved = service.create(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @PatchMapping("/orders/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateOrderStatusRequest
    ): OrderResponse {
        val updated = service.updateStatus(id, request.status)
        return updated.toResponse()
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/accept")
    fun acceptOrder(
        @PathVariable restaurantId: UUID,
        @PathVariable orderId: UUID
    ): ResponseEntity<OrderResponse> {
        val updated = service.acceptOrder(restaurantId, orderId)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/reject")
    fun rejectOrder(
        @PathVariable restaurantId: UUID,
        @PathVariable orderId: UUID
    ): ResponseEntity<OrderResponse> {
        val updated = service.rejectOrder(restaurantId, orderId)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PatchMapping("/orders/{orderId}/assign-driver")
    fun assignDriver(
        @PathVariable orderId: UUID,
        @RequestParam driverId: UUID
    ): ResponseEntity<OrderResponse> {
        val updated = service.assignDriver(orderId, driverId)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PatchMapping("/orders/{orderId}/pickup")
    fun confirmPickup(
        @PathVariable orderId: UUID,
        @RequestParam driverId: UUID
    ): ResponseEntity<OrderResponse> {
        val updated = service.confirmPickup(orderId, driverId)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PatchMapping("/orders/{orderId}/deliver")
    fun confirmDelivery(
        @PathVariable orderId: UUID,
        @RequestParam driverId: UUID
    ): ResponseEntity<OrderResponse> {
        val updated = service.confirmDelivery(orderId, driverId)
        return ResponseEntity.ok(updated.toResponse())
    }
}

