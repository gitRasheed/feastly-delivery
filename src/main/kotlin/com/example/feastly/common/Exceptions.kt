package com.example.feastly.common

import com.example.feastly.order.OrderStatus
import java.util.UUID

sealed class NotFoundException(message: String) : RuntimeException(message)

class UserNotFoundException(userId: UUID) : NotFoundException("User not found: $userId")

class RestaurantNotFoundException(restaurantId: UUID) : NotFoundException("Restaurant not found: $restaurantId")

class OrderNotFoundException(orderId: UUID) : NotFoundException("Order not found: $orderId")

class MenuItemNotFoundException(menuItemId: UUID) : NotFoundException("Menu item not found: $menuItemId")

class OrderAlreadyFinalizedException(status: OrderStatus) :
    IllegalStateException("Cannot modify order with status $status")

class UnauthorizedRestaurantAccessException(restaurantId: UUID) :
    RuntimeException("Restaurant $restaurantId is not authorized to modify this order")

class DriverAlreadyAssignedException(orderId: UUID) :
    IllegalStateException("Order $orderId already has a driver assigned")

class InvalidOrderStateForDispatchException(orderId: UUID, status: OrderStatus) :
    IllegalStateException("Order $orderId is not in a dispatchable state: $status")

class UnauthorizedDriverAccessException(driverId: UUID) :
    RuntimeException("Driver $driverId is not assigned to this order")

class OrderAlreadyDeliveredException(orderId: UUID) :
    IllegalStateException("Order $orderId has already been delivered or cancelled")

class InvalidDeliveryStateException(orderId: UUID, status: OrderStatus) :
    IllegalStateException("Order $orderId is not in DISPATCHED state, current: $status")

class MenuItemUnavailableException(itemId: UUID) :
    IllegalStateException("Menu item $itemId is unavailable")

class PaymentFailedException(message: String) : RuntimeException(message)

class RefundNotAllowedException(orderId: UUID) :
    IllegalStateException("Order $orderId cannot be refunded - payment was not successful")

