package com.example.feastly.saga

import com.example.feastly.BaseIntegrationTest
import com.example.feastly.menu.MenuItemRequest
import com.example.feastly.menu.MenuItemResponse
import com.example.feastly.order.CreateOrderRequest
import com.example.feastly.order.OrderItemRequest
import com.example.feastly.order.OrderResponse
import com.example.feastly.order.OrderStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Integration test verifying the complete saga flow from order creation to delivery.
 * 
 * This test simulates the saga events that would normally come from external services
 * (restaurant-service, dispatch-service) to verify order status transitions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SagaFlowIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var sagaManager: OrderSagaManager

    private fun url(path: String) = "http://localhost:$port$path"

    private fun createMenuItem(restaurantId: UUID): UUID {
        val req = MenuItemRequest(name = "Test Item", priceCents = 1000)
        val response = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        )
        return response.body!!.id
    }

    private fun createOrder(userId: UUID, restaurantId: UUID, menuItemId: UUID): OrderResponse {
        val headers = HttpHeaders().apply {
            set("X-USER-ID", userId.toString())
        }
        val request = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(OrderItemRequest(menuItemId = menuItemId, quantity = 1))
        )
        val response = restTemplate.postForEntity(
            url("/api/orders"),
            HttpEntity(request, headers),
            OrderResponse::class.java
        )
        assertEquals(HttpStatus.CREATED, response.statusCode)
        return response.body!!
    }

    private fun getOrderStatus(orderId: UUID, userId: UUID): OrderStatus {
        val headers = HttpHeaders().apply {
            set("X-USER-ID", userId.toString())
        }
        val response = restTemplate.exchange(
            url("/api/orders"),
            org.springframework.http.HttpMethod.GET,
            HttpEntity<Void>(headers),
            Array<OrderResponse>::class.java
        )
        return response.body!!.find { it.id == orderId }?.status ?: throw IllegalStateException("Order not found")
    }

    @Test
    fun `complete saga flow - order to DELIVERED`() {
        // Setup
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)

        // Step 1: Create order
        val order = createOrder(userId, restaurantId, menuItemId)
        assertEquals(OrderStatus.SUBMITTED, order.status)

        // Step 2: Simulate RestaurantOrderAcceptedEvent
        val restaurantAcceptedEvent = """
            {"orderId":"${order.id}","restaurantId":"$restaurantId","timestamp":"${java.time.Instant.now()}"}
        """.trimIndent()
        sagaManager.onRestaurantOrderAccepted(restaurantAcceptedEvent)

        // Verify status is AWAITING_DRIVER
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.AWAITING_DRIVER, getOrderStatus(order.id, userId))
        }

        // Step 3: Simulate DriverAssignedEvent
        val driverAssignedEvent = """
            {"orderId":"${order.id}","driverId":"$driverId","timestamp":"${java.time.Instant.now()}"}
        """.trimIndent()
        sagaManager.onDispatchEvent(driverAssignedEvent)

        // Verify status is DRIVER_ASSIGNED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DRIVER_ASSIGNED, getOrderStatus(order.id, userId))
        }

        // Step 4: Simulate DeliveryCompletedEvent
        val deliveryCompletedEvent = """
            {"orderId":"${order.id}","driverId":"$driverId","timestamp":"${java.time.Instant.now()}"}
        """.trimIndent()
        sagaManager.onDispatchEvent(deliveryCompletedEvent)

        // Verify final status is DELIVERED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DELIVERED, getOrderStatus(order.id, userId))
        }
    }

    @Test
    fun `delivery failed marks order as DISPATCH_FAILED`() {
        // Setup
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)

        // Create order
        val order = createOrder(userId, restaurantId, menuItemId)

        // Simulate restaurant acceptance and driver assignment
        sagaManager.onRestaurantOrderAccepted("""
            {"orderId":"${order.id}","restaurantId":"$restaurantId","timestamp":"${java.time.Instant.now()}"}
        """.trimIndent())

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.AWAITING_DRIVER, getOrderStatus(order.id, userId))
        }

        sagaManager.onDispatchEvent("""
            {"orderId":"${order.id}","driverId":"$driverId","timestamp":"${java.time.Instant.now()}"}
        """.trimIndent())

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DRIVER_ASSIGNED, getOrderStatus(order.id, userId))
        }

        // Simulate delivery failure
        val deliveryFailedEvent = """
            {"orderId":"${order.id}","driverId":"$driverId","reason":"Customer not available","timestamp":"${java.time.Instant.now()}"}
        """.trimIndent()
        sagaManager.onDispatchEvent(deliveryFailedEvent)

        // Verify final status is DISPATCH_FAILED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DISPATCH_FAILED, getOrderStatus(order.id, userId))
        }
    }
}
