package com.feastly.events

import java.time.Instant
import java.util.UUID

/**
 * Published when a new order is placed.
 */
data class OrderPlacedEvent(
    val orderId: UUID,
    val userId: UUID,
    val totalCents: Int,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Published when a restaurant accepts an order.
 */
data class OrderAcceptedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Published when a new restaurant is created.
 */
data class RestaurantCreatedEvent(
    val restaurantId: UUID,
    val name: String,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Command sent to restaurant to prepare an order.
 */
data class RestaurantOrderRequest(
    val orderId: UUID,
    val restaurantId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Event when restaurant accepts order via saga flow.
 */
data class RestaurantOrderAcceptedEvent(
    val orderId: UUID,
    val restaurantId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Command sent to dispatch to assign a driver.
 */
data class AssignDriverCommand(
    val orderId: UUID,
    val restaurantId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Event when driver is assigned to an order.
 */
data class DriverAssignedEvent(
    val orderId: UUID,
    val driverId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Event when delivery is completed successfully.
 */
data class DeliveryCompletedEvent(
    val orderId: UUID,
    val driverId: UUID,
    override val timestamp: Instant = Instant.now()
) : DomainEvent

/**
 * Event when driver fails to complete delivery.
 */
data class DriverDeliveryFailedEvent(
    val orderId: UUID,
    val driverId: UUID,
    val reason: String,
    override val timestamp: Instant = Instant.now()
) : DomainEvent
