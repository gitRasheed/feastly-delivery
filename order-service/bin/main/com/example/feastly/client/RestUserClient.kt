package com.example.feastly.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.util.UUID

@Component
class RestUserClient(
    private val restTemplate: RestTemplate,
    @Value("\${services.user.url:http://localhost:8083}") private val baseUrl: String
) : UserClient {

    override fun existsById(userId: UUID): Boolean {
        return try {
            restTemplate.getForEntity<Any>("$baseUrl/api/users/$userId")
            true
        } catch (_: Exception) {
            false
        }
    }
}
