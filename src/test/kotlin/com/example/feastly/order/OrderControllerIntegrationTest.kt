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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    private fun headersWithUser(userId: UUID): HttpHeaders {
        val headers = HttpHeaders()
        headers.set("X-USER-ID", userId.toString())
        headers.set("Content-Type", "application/json")
        return headers
    }

    @Test
    fun create_update_list_order_flow() {
        val userReq = UserRegisterRequest(email = "orderuser@example.com", password = "secret123")
        val userRes = restTemplate.postForEntity(url("/api/users"), userReq, UserResponse::class.java)
        assertEquals(HttpStatus.CREATED, userRes.statusCode)
        val userId: UUID = userRes.body!!.id

        val restReq = RestaurantRegisterRequest(
            name = "Sushi Spot",
            address = "1 Ocean Ave",
            cuisine = "Japanese"
        )
        val restRes = restTemplate.postForEntity(
            url("/api/restaurants"),
            restReq,
            RestaurantResponse::class.java
        )
        assertEquals(HttpStatus.CREATED, restRes.statusCode)
        val restaurantId: UUID = restRes.body!!.id

        val createOrder = CreateOrderRequest(restaurantId = restaurantId)
        val orderRes = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
        )
        assertEquals(HttpStatus.CREATED, orderRes.statusCode)
        val createdOrder = orderRes.body!!
        assertEquals(OrderStatus.AWAITING_RESTAURANT, createdOrder.status)
        assertEquals(userId, createdOrder.userId)

        val patchReq = UpdateOrderStatusRequest(status = OrderStatus.PENDING)
        val updated = restTemplate.patchForObject(
            url("/api/orders/${createdOrder.id}/status"),
            patchReq,
            OrderResponse::class.java
        )!!
        assertEquals(OrderStatus.PENDING, updated.status)

        val myOrdersResponse = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.GET,
            HttpEntity<Void>(headersWithUser(userId)),
            Array<OrderResponse>::class.java
        )
        assertEquals(HttpStatus.OK, myOrdersResponse.statusCode)
        assertTrue(myOrdersResponse.body!!.any { it.id == createdOrder.id })
    }

    @Test
    fun create_order_with_invalid_user_returns_404() {
        val fakeUserId = UUID.randomUUID()
        val createOrder = CreateOrderRequest(restaurantId = UUID.randomUUID())
        val response = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(fakeUserId)),
            String::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
