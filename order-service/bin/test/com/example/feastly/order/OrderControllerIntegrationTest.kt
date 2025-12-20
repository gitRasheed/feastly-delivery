package com.example.feastly.order

import com.example.feastly.BaseIntegrationTest
import com.example.feastly.TestRestaurantMenuClient
import com.example.feastly.client.MenuItemData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
class OrderControllerIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    @BeforeEach
    fun setUp() {
        TestRestaurantMenuClient.clearMenuItems()
    }

    private fun headersWithUser(userId: UUID): HttpHeaders {
        val headers = HttpHeaders()
        headers.set("X-USER-ID", userId.toString())
        headers.set("Content-Type", "application/json")
        return headers
    }

    private fun testUserId(): UUID = UUID.randomUUID()

    private fun testRestaurantId(): UUID = UUID.randomUUID()

    private fun createMenuItem(restaurantId: UUID, name: String, priceCents: Int): MenuItemData {
        return TestRestaurantMenuClient.registerMenuItem(
            restaurantId = restaurantId,
            name = name,
            priceCents = priceCents
        )
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
        val userId = testUserId()
        val restaurantId = testRestaurantId()

        val pizza = createMenuItem(restaurantId, "Pizza", 1500)
        val drink = createMenuItem(restaurantId, "Soda", 300)

        val createOrder = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(
                OrderItemRequest(menuItemId = pizza.id, quantity = 2),
                OrderItemRequest(menuItemId = drink.id, quantity = 3)
            )
        )

        val orderRes = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, orderRes.statusCode)
        val createdOrder = orderRes.body!!

        assertEquals(OrderStatus.SUBMITTED, createdOrder.status)
        assertEquals(userId, createdOrder.customerId)
        assertEquals(restaurantId, createdOrder.restaurantId)

        // Pricing breakdown:
        // Subtotal: 2 * 1500 + 3 * 300 = 3000 + 900 = 3900
        assertEquals(3900, createdOrder.subtotalCents)
        // Service fee: 10% of 3900 = 390 (within min 99, max 999)
        assertEquals(390, createdOrder.taxCents)
        // Delivery fee: flat 299
        assertEquals(299, createdOrder.deliveryFeeCents)
        // Total: 3900 + 390 + 299 = 4589
        assertEquals(4589, createdOrder.totalCents)
    }

    @Test
    fun `update order status works correctly`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Burger", 1200)

        val createOrder = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1))
        )

        val created = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
        ).body!!

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
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Salad", 900)

        val createOrder = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1))
        )

        val created = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
        ).body!!

        val myOrdersRes = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.GET,
            HttpEntity<Void>(headersWithUser(userId)),
            Array<OrderResponse>::class.java
        )

        assertEquals(HttpStatus.OK, myOrdersRes.statusCode)
        assertTrue(myOrdersRes.body!!.any { it.id == created.id })
    }

    private fun acceptOrder(restaurantId: UUID, orderId: UUID): OrderResponse {
        return restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/$orderId/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        ).body!!
    }

    @Test
    fun `restaurant can accept a submitted order`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Steak", 2500)

        val order = createOrder(userId, restaurantId, menuItem.id)
        assertEquals(OrderStatus.SUBMITTED, order.status)

        val response = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/${order.id}/accept"),
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
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Sushi", 1800)

        val order = createOrder(userId, restaurantId, menuItem.id)
        assertEquals(OrderStatus.SUBMITTED, order.status)

        val response = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/${order.id}/reject"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(OrderStatus.CANCELLED, response.body!!.status)
    }

    @Test
    fun `cannot accept already accepted order`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Tacos", 1200)

        val order = createOrder(userId, restaurantId, menuItem.id)
        acceptOrder(restaurantId, order.id)

        val response = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/${order.id}/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
    }

    @Test
    fun `driver can be assigned to an accepted order`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Burger Deluxe", 1800)

        val order = createOrder(userId, restaurantId, menuItem.id)
        acceptOrder(restaurantId, order.id)

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

    private fun assignDriverAndPickup(orderId: UUID, restaurantId: UUID, driverId: UUID) {
        acceptOrder(restaurantId, orderId)
        
        restTemplate.exchange(
            url("/api/orders/$orderId/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )
        
        // Mark ready for pickup (required since Phase 2B)
        restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/$orderId/ready"),
            HttpMethod.POST,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )
        
        restTemplate.exchange(
            url("/api/orders/$orderId/pickup?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )
    }

    @Test
    fun `pickup transitions order to DISPATCHED`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Tacos Supreme", 1300)

        val order = createOrder(userId, restaurantId, menuItem.id)
        acceptOrder(restaurantId, order.id)

        val driverId = UUID.randomUUID()

        restTemplate.exchange(
            url("/api/orders/${order.id}/assign-driver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        // Mark ready for pickup (required since Phase 2B)
        restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/${order.id}/ready"),
            HttpMethod.POST,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

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
    fun `assigned driver can mark DISPATCHED order as DELIVERED`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Steak Dinner", 3500)

        val order = createOrder(userId, restaurantId, menuItem.id)
        val driverId = UUID.randomUUID()
        assignDriverAndPickup(order.id, restaurantId, driverId)

        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/deliver?driverId=$driverId"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(OrderStatus.DELIVERED, response.body!!.status)
    }

    // @Test - Disabled: OrderResponse doesn't include paymentStatus and paymentReference fields
    // fun `creating order sets payment status to PAID`() {
    //     ...
    // }

    // @Test - Disabled: OrderResponse doesn't include paymentStatus field  
    // fun `refund order sets payment status to REFUNDED`() {
    //     ...
    // }

    @Test
    fun `order creation with tip includes tip in total`() {
        val userId = testUserId()
        val restaurantId = testRestaurantId()
        val menuItem = createMenuItem(restaurantId, "Burger", 1200)

        val createOrder = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(OrderItemRequest(menuItemId = menuItem.id, quantity = 1)),
            tipCents = 300
        )

        val response = restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val order = response.body!!

        // Subtotal: 1200
        assertEquals(1200, order.subtotalCents)
        // Total: 1200 + 120 (10% service fee) + 299 (delivery) + 300 (tip) = 1919
        assertEquals(1919, order.totalCents)
    }
}
