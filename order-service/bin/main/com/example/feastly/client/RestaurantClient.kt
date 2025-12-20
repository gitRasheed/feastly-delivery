package com.example.feastly.client

import java.util.UUID

interface RestaurantClient {
    fun existsById(restaurantId: UUID): Boolean
}
