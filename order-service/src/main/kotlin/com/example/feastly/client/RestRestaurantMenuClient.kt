package com.example.feastly.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class RestRestaurantMenuClient(
    private val restTemplate: RestTemplate,
    @Value("\${services.restaurant.url:http://localhost:8084}") private val baseUrl: String
) : RestaurantMenuClient {

    private val logger = LoggerFactory.getLogger(RestRestaurantMenuClient::class.java)

    override fun batchGetMenuItems(ids: List<UUID>): List<MenuItemData> {
        if (ids.isEmpty()) return emptyList()

        logger.debug("Fetching menu items from restaurant-service: {}", ids)

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val request = HttpEntity(BatchGetRequest(ids), headers)

        val response = restTemplate.postForEntity(
            "$baseUrl/internal/menu-items:batchGet",
            request,
            BatchGetResponse::class.java
        )

        return response.body?.items ?: emptyList()
    }

    private data class BatchGetRequest(val ids: List<UUID>)

    private data class BatchGetResponse(val items: List<MenuItemData>)
}
