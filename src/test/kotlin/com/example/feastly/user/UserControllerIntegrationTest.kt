package com.example.feastly.user

import com.example.feastly.order.CreateOrderRequest
import com.example.feastly.order.OrderResponse
import com.example.feastly.order.OrderHistoryResponse
import com.example.feastly.rating.RatingRequest
import com.example.feastly.restaurant.RestaurantRegisterRequest
import com.example.feastly.restaurant.RestaurantResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
class UserControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    private fun createUser(email: String): UserResponse {
        val req = UserRegisterRequest(email = email, password = "secret123")
        val res = restTemplate.postForEntity(url("/api/users"), req, UserResponse::class.java)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        return res.body!!
    }

    private fun createRestaurant(): RestaurantResponse {
        val req = RestaurantRegisterRequest(
            name = "Test Restaurant",
            address = "123 Main St",
            cuisine = "Italian"
        )
        val res = restTemplate.postForEntity(url("/api/restaurants"), req, RestaurantResponse::class.java)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        return res.body!!
    }

    private fun createOrder(userId: UUID, restaurantId: UUID): OrderResponse {
        val headers = HttpHeaders()
        headers.set("X-USER-ID", userId.toString())
        headers.set("Content-Type", "application/json")
        
        val req = CreateOrderRequest(restaurantId = restaurantId)
        val res = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(req, headers),
            OrderResponse::class.java
        )
        assertEquals(HttpStatus.CREATED, res.statusCode)
        return res.body!!
    }

    @Test
    fun `save address returns 204 for valid user`() {
        val user = createUser("address-test@example.com")
        
        val addressReq = AddressRequest(
            line1 = "42 Oak Street",
            city = "London",
            postcode = "SW1A 1AA"
        )
        
        val res = restTemplate.postForEntity(
            url("/api/users/${user.id}/address"),
            addressReq,
            Void::class.java
        )
        
        assertEquals(HttpStatus.NO_CONTENT, res.statusCode)
    }

    @Test
    fun `save address returns 404 for non-existent user`() {
        val fakeUserId = UUID.randomUUID()
        val addressReq = AddressRequest(
            line1 = "42 Oak Street",
            city = "London",
            postcode = "SW1A 1AA"
        )
        
        val res = restTemplate.postForEntity(
            url("/api/users/$fakeUserId/address"),
            addressReq,
            String::class.java
        )
        
        assertEquals(HttpStatus.NOT_FOUND, res.statusCode)
    }

    @Test
    fun `get order history returns orders for user`() {
        val user = createUser("history-test@example.com")
        val restaurant = createRestaurant()
        val order = createOrder(user.id, restaurant.id)
        
        val res = restTemplate.getForEntity(
            url("/api/users/${user.id}/orders"),
            Array<OrderHistoryResponse>::class.java
        )
        
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertTrue(res.body!!.any { it.orderId == order.id })
        assertTrue(res.body!!.any { it.restaurantName == "Test Restaurant" })
    }

    @Test
    fun `rate order returns 204 for valid rating`() {
        val user = createUser("rating-test@example.com")
        val restaurant = createRestaurant()
        val order = createOrder(user.id, restaurant.id)
        
        val ratingReq = RatingRequest(stars = 5, comment = "Great food!")
        
        val res = restTemplate.postForEntity(
            url("/api/users/${user.id}/orders/${order.id}/rating"),
            ratingReq,
            Void::class.java
        )
        
        assertEquals(HttpStatus.NO_CONTENT, res.statusCode)
    }

    @Test
    fun `rate order returns 404 for non-existent order`() {
        val user = createUser("rating-404-test@example.com")
        val fakeOrderId = UUID.randomUUID()
        
        val ratingReq = RatingRequest(stars = 4, comment = "Good")
        
        val res = restTemplate.postForEntity(
            url("/api/users/${user.id}/orders/$fakeOrderId/rating"),
            ratingReq,
            String::class.java
        )
        
        assertEquals(HttpStatus.NOT_FOUND, res.statusCode)
    }
}
