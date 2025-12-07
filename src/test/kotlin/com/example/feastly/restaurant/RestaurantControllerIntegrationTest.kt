package com.example.feastly.restaurant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RestaurantControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    @Test
    fun register_and_list_restaurants() {
        val req = RestaurantRegisterRequest(
            name = "Pasta Place",
            address = "123 Main St",
            cuisine = "Italian"
        )
        val postResponse = restTemplate.postForEntity(url("/api/restaurants"), req, RestaurantResponse::class.java)
        assertEquals(HttpStatus.CREATED, postResponse.statusCode)
        val created = postResponse.body!!
        assertEquals("Pasta Place", created.name)

        val listResponse = restTemplate.exchange(
            url("/api/restaurants"), HttpMethod.GET, HttpEntity.EMPTY, Array<RestaurantResponse>::class.java
        )
        assertEquals(HttpStatus.OK, listResponse.statusCode)
        assertTrue(listResponse.body!!.any { it.id == created.id })
    }
}
