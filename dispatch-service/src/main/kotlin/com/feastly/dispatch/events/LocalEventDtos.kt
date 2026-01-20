package com.feastly.dispatch.events

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant
import java.util.UUID

/**
 * Local event DTOs for dispatch-service.
 * These are decoupled from order-service and use @JsonIgnoreProperties for forward compatibility.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderAcceptedEventDto(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssignDriverCommandDto(
    val orderId: UUID,
    val restaurantId: UUID,
    val timestamp: Instant? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DriverAssignedEventDto(
    val orderId: UUID,
    val driverId: UUID,
    val timestamp: Instant? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeliveryCompletedEventDto(
    val orderId: UUID,
    val driverId: UUID,
    val timestamp: Instant? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DriverDeliveryFailedEventDto(
    val orderId: UUID,
    val driverId: UUID,
    val reason: String,
    val timestamp: Instant? = null
)
