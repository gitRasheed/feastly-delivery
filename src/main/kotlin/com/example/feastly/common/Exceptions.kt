package com.example.feastly.common

import java.util.UUID

sealed class NotFoundException(message: String) : RuntimeException(message)

class UserNotFoundException(userId: UUID) : NotFoundException("User not found: $userId")

class RestaurantNotFoundException(restaurantId: UUID) : NotFoundException("Restaurant not found: $restaurantId")

class OrderNotFoundException(orderId: UUID) : NotFoundException("Order not found: $orderId")
