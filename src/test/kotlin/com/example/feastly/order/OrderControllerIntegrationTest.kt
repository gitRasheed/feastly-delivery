package com.example.feastly.order

import com.example.feastly.restaurant.RestaurantRegisterRequest
import com.example.feastly.restaurant.RestaurantResponse
import com.example.feastly.user.UserRegisterRequest
import com.example.feastly.user.UserResponse
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
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerIntegrationTest {

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
    fun create_update_list_order_flow() {
        // Create user
        val userReq = UserRegisterRequest(email = "orderuser@example.com", password = "secret123")
        val userRes = restTemplate.postForEntity(url("/api/users"), userReq, UserResponse::class.java)
        assertEquals(HttpStatus.CREATED, userRes.statusCode)
        val userId: UUID = userRes.body!!.id

        // Create restaurant
        val restReq = RestaurantRegisterRequest(name = "Sushi Spot", address = "1 Ocean Ave", cuisine = "Japanese")
        val restRes = restTemplate.postForEntity(url("/api/restaurants"), restReq, RestaurantResponse::class.java)
        assertEquals(HttpStatus.CREATED, restRes.statusCode)
        val restaurantId: UUID = restRes.body!!.id

        // Create order
        val createOrder = CreateOrderRequest(userId = userId, restaurantId = restaurantId)
        val orderRes = restTemplate.postForEntity(url("/api/orders"), createOrder, OrderResponse::class.java)
        assertEquals(HttpStatus.CREATED, orderRes.statusCode)
        val createdOrder = orderRes.body!!
        assertEquals(OrderStatus.AWAITING_RESTAURANT, createdOrder.status)

        // Update status -> PENDING
        val patchReq = UpdateOrderStatusRequest(status = OrderStatus.PENDING)
        val updated = restTemplate.patchForObject(url("/api/orders/${createdOrder.id}/status"), patchReq, OrderResponse::class.java)!!
        assertEquals(OrderStatus.PENDING, updated.status)

        // List by user
        val listResponse = restTemplate.exchange(
            url("/api/orders/user/$userId"), HttpMethod.GET, HttpEntity.EMPTY, Array<OrderResponse>::class.java
        )
        assertEquals(HttpStatus.OK, listResponse.statusCode)
        assertTrue(listResponse.body!!.any { it.id == createdOrder.id })
    }
}

