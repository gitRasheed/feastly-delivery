package com.example.feastly.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import java.time.Instant
import java.util.UUID

@Component
class RestRestaurantAvailabilityClient(
    private val restTemplate: RestTemplate,
    @Value("\${services.restaurant.url:http://localhost:8094}") private val baseUrl: String
) : RestaurantAvailabilityClient {

    private val logger = LoggerFactory.getLogger(RestRestaurantAvailabilityClient::class.java)

    override fun checkAvailability(restaurantId: UUID, at: Instant): RestaurantAvailabilityResponse {
        val url = "$baseUrl/internal/restaurants/$restaurantId/availability?at=$at"
        logger.debug("Checking availability for restaurant {} at {}", restaurantId, at)
        
        return restTemplate.getForObject<RestaurantAvailabilityResponse>(url)
    }
}
