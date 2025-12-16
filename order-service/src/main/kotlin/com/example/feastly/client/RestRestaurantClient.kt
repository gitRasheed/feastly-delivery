package com.example.feastly.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.util.UUID

@Component
class RestRestaurantClient(
    private val restTemplate: RestTemplate,
    @Value("\${services.restaurant.url:http://localhost:8084}") private val baseUrl: String
) : RestaurantClient {

    override fun existsById(restaurantId: UUID): Boolean {
        return try {
            restTemplate.getForEntity<Any>("$baseUrl/api/restaurants/$restaurantId")
            true
        } catch (_: Exception) {
            false
        }
    }
}
