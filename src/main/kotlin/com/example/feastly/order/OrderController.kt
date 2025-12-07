package com.example.feastly.order

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val service: OrderService
) {
    companion object {
        const val USER_ID_HEADER = "X-USER-ID"
    }

    @GetMapping
    fun getMyOrders(@RequestHeader(USER_ID_HEADER) userId: UUID): List<OrderResponse> =
        service.getOrdersForUser(userId).map { it.toResponse() }

    @PostMapping
    fun create(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val saved = service.create(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateOrderStatusRequest
    ): OrderResponse {
        val updated = service.updateStatus(id, request.status)
        return updated.toResponse()
    }

}