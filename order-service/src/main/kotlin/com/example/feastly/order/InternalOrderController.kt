package com.example.feastly.order

import com.feastly.events.OrderInfo
import com.feastly.events.OrderStatus
import com.feastly.security.InternalApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Internal API endpoints for cross-service communication.
 * 
 * **IMPORTANT: For internal service-to-service use only.**
 * Not intended for external clients. Use Kafka events as primary integration.
 */
@InternalApi(reason = "Service-to-service communication for dispatch workflow")
@RestController
@RequestMapping("/api/internal")
class InternalOrderController(
    private val orderRepository: OrderRepository
) {
    companion object {
        // Default location (NYC) - TODO: Replace with actual restaurant location from DB
        private const val DEFAULT_LAT = 40.7128
        private const val DEFAULT_LNG = -74.0060
    }

    /**
     * Returns order info in format suitable for dispatch-service.
     * Fallback endpoint for operational tooling and admin dashboards.
     */
    @InternalApi(reason = "Provides order data for dispatch-service fallback queries")
    @GetMapping("/orders/{orderId}/dispatch-info")
    fun getDispatchInfo(@PathVariable orderId: UUID): ResponseEntity<OrderInfo> {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val orderInfo = OrderInfo(
            orderId = order.id,
            status = OrderStatus.valueOf(order.status.name),
            assignedDriverId = order.driverId,
            // TODO: Add lat/lng to Restaurant entity and retrieve actual values
            restaurantLat = DEFAULT_LAT,
            restaurantLng = DEFAULT_LNG
        )

        return ResponseEntity.ok(orderInfo)
    }
}
