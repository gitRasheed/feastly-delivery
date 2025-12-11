package com.example.feastly.order

import com.example.feastly.menu.MenuItemRequest
import com.example.feastly.menu.MenuItemResponse
import com.example.feastly.restaurant.RestaurantRegisterRequest
import com.example.feastly.restaurant.RestaurantResponse
import com.example.feastly.user.UserRegisterRequest
import com.example.feastly.user.UserResponse
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

    private fun createUser(email: String): UserResponse {
        val req = UserRegisterRequest(email = email, password = "secret123")
        return restTemplate.postForEntity(url("/api/users"), req, UserResponse::class.java).body!!
    }

    private fun createRestaurant(name: String): RestaurantResponse {
        val req = RestaurantRegisterRequest(name = name, address = "123 Test St", cuisine = "Test")
        return restTemplate.postForEntity(url("/api/restaurants"), req, RestaurantResponse::class.java).body!!
    }

    private fun createMenuItem(restaurantId: UUID, name: String, priceCents: Int): MenuItemResponse {
        val req = MenuItemRequest(name = name, priceCents = priceCents)
        return restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        ).body!!
    }

    @Test
    fun `create order with items calculates total correctly`() {
        val user = createUser("order-items-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Test Restaurant ${UUID.randomUUID()}")

        // Create menu items
        val pizza = createMenuItem(restaurant.id, "Pizza", 1500)
        val drink = createMenuItem(restaurant.id, "Soda", 300)

        // Create order with items
        val createOrder = CreateOrderRequest(
            restaurantId = restaurant.id,
            items = listOf(
                OrderItemRequest(menuItemId = pizza.id, quantity = 2),
                OrderItemRequest(menuItemId = drink.id, quantity = 3)
            )
        )

        val orderRes = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(user.id)),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, orderRes.statusCode)
        val createdOrder = orderRes.body!!

        assertEquals(OrderStatus.AWAITING_RESTAURANT, createdOrder.status)
        assertEquals(user.id, createdOrder.userId)
        assertEquals(restaurant.id, createdOrder.restaurantId)

        // Total: 2 * 1500 + 3 * 300 = 3000 + 900 = 3900
        assertEquals(3900, createdOrder.totalCents)
        assertEquals(2, createdOrder.items.size)

        val pizzaItem = createdOrder.items.find { it.menuItemName == "Pizza" }
        assertNotNull(pizzaItem)
        assertEquals(2, pizzaItem!!.quantity)
        assertEquals(1500, pizzaItem.priceCents)
        assertEquals(3000, pizzaItem.lineTotalCents)
    }

    @Test
    fun `update order status works correctly`() {
        val user = createUser("order-status-test@example.com")
        val restaurant = createRestaurant("Status Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Burger", 1200)

        val createOrder = CreateOrderRequest(
            restaurantId = restaurant.id,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1))
        )

        val created = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(user.id)),
            OrderResponse::class.java
        ).body!!

        // Update status
        val patchReq = UpdateOrderStatusRequest(status = OrderStatus.PENDING)
        val updated = restTemplate.patchForObject(
            url("/api/orders/${created.id}/status"),
            patchReq,
            OrderResponse::class.java
        )!!

        assertEquals(OrderStatus.PENDING, updated.status)
    }

    @Test
    fun `get my orders returns user orders`() {
        val user = createUser("my-orders-test@example.com")
        val restaurant = createRestaurant("My Orders Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Salad", 900)

        val createOrder = CreateOrderRequest(
            restaurantId = restaurant.id,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1))
        )

        val created = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(user.id)),
            OrderResponse::class.java
        ).body!!

        val myOrdersRes = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.GET,
            HttpEntity<Void>(headersWithUser(user.id)),
            Array<OrderResponse>::class.java
        )

        assertEquals(HttpStatus.OK, myOrdersRes.statusCode)
        assertTrue(myOrdersRes.body!!.any { it.id == created.id })
    }

    @Test
    fun `create order with invalid user returns 404`() {
        val fakeUserId = UUID.randomUUID()
        val createOrder = CreateOrderRequest(
            restaurantId = UUID.randomUUID(),
            items = listOf(OrderItemRequest(menuItemId = UUID.randomUUID(), quantity = 1))
        )

        val response = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(fakeUserId)),
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `create order with menu item from different restaurant fails`() {
        val user = createUser("wrong-restaurant-test@example.com")
        val restaurant1 = createRestaurant("Restaurant 1 ${UUID.randomUUID()}")
        val restaurant2 = createRestaurant("Restaurant 2 ${UUID.randomUUID()}")

        // Create item in restaurant 1
        val menuItem = createMenuItem(restaurant1.id, "Pasta", 1100)

        // Try to order from restaurant 2 with restaurant 1's menu item
        val createOrder = CreateOrderRequest(
            restaurantId = restaurant2.id,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1))
        )

        val response = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(user.id)),
            String::class.java
        )

        // Should fail validation (500 from require() or 400 from validation)
        assertTrue(response.statusCode.is4xxClientError || response.statusCode.is5xxServerError)
    }
}

