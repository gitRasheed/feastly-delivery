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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RestaurantControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

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

