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

    private fun createOrder(userId: UUID, restaurantId: UUID, menuItemId: UUID): OrderResponse {
        val createOrder = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(OrderItemRequest(menuItemId = menuItemId, quantity = 1))
        )
        return restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
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

        assertEquals(OrderStatus.SUBMITTED, createdOrder.status)
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
        val user = createUser("order-status-test-${UUID.randomUUID()}@example.com")
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
        val patchReq = UpdateOrderStatusRequest(status = OrderStatus.ACCEPTED)
        val updated = restTemplate.patchForObject(
            url("/api/orders/${created.id}/status"),
            patchReq,
            OrderResponse::class.java
        )!!

        assertEquals(OrderStatus.ACCEPTED, updated.status)
    }

    @Test
    fun `get my orders returns user orders`() {
        val user = createUser("my-orders-test-${UUID.randomUUID()}@example.com")
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
        val user = createUser("wrong-restaurant-test-${UUID.randomUUID()}@example.com")
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

    // ========== Restaurant Accept/Reject Tests ==========

    @Test
    fun `restaurant can accept a submitted order`() {
        val user = createUser("accept-order-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Accept Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Steak", 2500)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        assertEquals(OrderStatus.SUBMITTED, order.status)

        val response = restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/orders/${order.id}/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(OrderStatus.ACCEPTED, response.body!!.status)
        assertEquals(order.id, response.body!!.id)
    }

    @Test
    fun `restaurant can reject a submitted order`() {
        val user = createUser("reject-order-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Reject Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Sushi", 1800)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        assertEquals(OrderStatus.SUBMITTED, order.status)

        val response = restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/orders/${order.id}/reject"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(OrderStatus.CANCELLED, response.body!!.status)
    }

    @Test
    fun `cannot accept already accepted order`() {
        val user = createUser("double-accept-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Double Accept Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Tacos", 1200)

        val order = createOrder(user.id, restaurant.id, menuItem.id)

        // Accept first time
        restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/orders/${order.id}/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Try to accept again
        val response = restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/orders/${order.id}/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    @Test
    fun `cannot accept cancelled order`() {
        val user = createUser("accept-cancelled-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Cancelled Order Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Ramen", 1400)

        val order = createOrder(user.id, restaurant.id, menuItem.id)

        // Reject the order first
        restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/orders/${order.id}/reject"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Try to accept the cancelled order
        val response = restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/orders/${order.id}/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    @Test
    fun `restaurant cannot modify another restaurant's order`() {
        val user = createUser("cross-restaurant-test-${UUID.randomUUID()}@example.com")
        val restaurant1 = createRestaurant("Restaurant Owner ${UUID.randomUUID()}")
        val restaurant2 = createRestaurant("Restaurant Attacker ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant1.id, "Pizza Special", 1600)

        val order = createOrder(user.id, restaurant1.id, menuItem.id)

        // Restaurant 2 tries to accept restaurant 1's order
        val response = restTemplate.exchange(
            url("/api/restaurants/${restaurant2.id}/orders/${order.id}/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    // ========== Driver Assignment Tests ==========

    private fun acceptOrder(restaurantId: UUID, orderId: UUID): OrderResponse {
        return restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/$orderId/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        ).body!!
    }

    @Test
    fun `driver can be assigned to an accepted order`() {
        val user = createUser("assign-driver-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Driver Test Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Burger Deluxe", 1800)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val driverId = UUID.randomUUID()
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(driverId, response.body!!.driverId)
        assertEquals(OrderStatus.ACCEPTED, response.body!!.status)
    }

    @Test
    fun `driver cannot be assigned if already assigned`() {
        val user = createUser("double-assign-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Double Assign Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Pizza Double", 1500)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val driverId1 = UUID.randomUUID()
        val driverId2 = UUID.randomUUID()

        // Assign first driver
        restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId1"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Try to assign second driver
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId2"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    @Test
    fun `only assigned driver can confirm pickup`() {
        val user = createUser("pickup-auth-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Pickup Auth Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Sushi Premium", 2200)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val assignedDriver = UUID.randomUUID()
        val wrongDriver = UUID.randomUUID()

        // Assign driver
        restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$assignedDriver"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Wrong driver tries to confirm pickup
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/pickup?driverId=$wrongDriver"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `pickup transitions order to DISPATCHED`() {
        val user = createUser("pickup-dispatch-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Dispatch Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Tacos Supreme", 1300)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val driverId = UUID.randomUUID()

        // Assign driver
        restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Confirm pickup
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/pickup?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(OrderStatus.DISPATCHED, response.body!!.status)
    }

    @Test
    fun `cannot assign driver to non-ACCEPTED order`() {
        val user = createUser("assign-submitted-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Submitted Order Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Curry Special", 1600)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        // Order is in SUBMITTED state, not ACCEPTED

        val driverId = UUID.randomUUID()
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    // ========== Delivery Completion Tests ==========

    private fun assignDriverAndPickup(orderId: UUID, restaurantId: UUID, driverId: UUID) {
        // Accept the order first
        acceptOrder(restaurantId, orderId)
        
        // Assign driver
        restTemplate.exchange(
            url("/api/orders/$orderId/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )
        
        // Confirm pickup
        restTemplate.exchange(
            url("/api/orders/$orderId/pickup?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )
    }

    @Test
    fun `assigned driver can mark DISPATCHED order as DELIVERED`() {
        val user = createUser("delivery-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Delivery Test Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Steak Dinner", 3500)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        val driverId = UUID.randomUUID()
        assignDriverAndPickup(order.id, restaurant.id, driverId)

        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/deliver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(OrderStatus.DELIVERED, response.body!!.status)
    }

    @Test
    fun `non-assigned driver cannot mark delivery complete`() {
        val user = createUser("wrong-driver-delivery-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Wrong Driver Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Pasta Carbonara", 1800)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        val assignedDriver = UUID.randomUUID()
        val wrongDriver = UUID.randomUUID()
        assignDriverAndPickup(order.id, restaurant.id, assignedDriver)

        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/deliver?driverId=$wrongDriver"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `cannot deliver non-DISPATCHED order`() {
        val user = createUser("non-dispatched-delivery-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Non-Dispatched Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Chicken Wings", 1200)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)
        
        val driverId = UUID.randomUUID()
        // Assign driver but don't confirm pickup - order stays in ACCEPTED
        restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/deliver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    @Test
    fun `cannot modify DELIVERED order afterward`() {
        val user = createUser("modify-delivered-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Modify Delivered Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Fish Tacos", 1400)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        val driverId = UUID.randomUUID()
        assignDriverAndPickup(order.id, restaurant.id, driverId)

        // Deliver the order
        restTemplate.exchange(
            url("/api/orders/${order.id}/deliver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Try to assign another driver - should fail
        val newDriverId = UUID.randomUUID()
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$newDriverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    // ========== Menu Item Availability Tests ==========

    @Test
    fun `ordering unavailable menu item returns 409`() {
        val user = createUser("unavailable-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Unavailable Test Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Sold Out Dish", 1500)

        // Mark item unavailable
        restTemplate.exchange(
            url("/api/restaurants/${restaurant.id}/menu/${menuItem.id}/availability?available=false"),
            HttpMethod.PATCH,
            HttpEntity.EMPTY,
            Void::class.java
        )

        // Try to order it
        val orderRequest = CreateOrderRequest(
            restaurantId = restaurant.id,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1))
        )

        val response = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(orderRequest, headersWithUser(user.id)),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertTrue(response.body!!.contains("unavailable"))
    }
}

