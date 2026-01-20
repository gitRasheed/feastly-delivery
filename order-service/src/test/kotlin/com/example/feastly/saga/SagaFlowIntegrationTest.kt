package com.example.feastly.saga

import com.example.feastly.BaseIntegrationTest
import com.example.feastly.TestRestaurantMenuClient
import com.example.feastly.order.CreateOrderRequest
import com.example.feastly.order.OrderItemRequest
import com.example.feastly.order.OrderResponse
import com.example.feastly.order.OrderStatus
import com.example.feastly.events.DeliveryCompletedEvent
import com.example.feastly.events.DriverAssignedEvent
import com.example.feastly.events.DriverDeliveryFailedEvent
import com.example.feastly.events.RestaurantOrderAcceptedEvent
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SagaFlowIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var sagaManager: OrderSagaManager

    private fun url(path: String) = "http://localhost:$port$path"

    @BeforeEach
    fun setUp() {
        TestRestaurantMenuClient.clearMenuItems()
    }

    private fun createMenuItem(restaurantId: UUID): UUID {
        val item = TestRestaurantMenuClient.registerMenuItem(
            restaurantId = restaurantId,
            name = "Test Item",
            priceCents = 1000
        )
        return item.id
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
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            Array<OrderResponse>::class.java
        )
        return response.body!!.find { it.id == orderId }?.status ?: throw IllegalStateException("Order not found")
    }

    @Test
    fun `complete saga flow - order to DELIVERED`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)

        val order = createOrder(userId, restaurantId, menuItemId)
        assertEquals(OrderStatus.SUBMITTED, order.status)

        sagaManager.onRestaurantEvent(RestaurantOrderAcceptedEvent(
            orderId = order.id,
            restaurantId = restaurantId
        ))

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.AWAITING_DRIVER, getOrderStatus(order.id, userId))
        }

        sagaManager.onDispatchEvent(DriverAssignedEvent(
            orderId = order.id,
            driverId = driverId
        ))

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DRIVER_ASSIGNED, getOrderStatus(order.id, userId))
        }

        sagaManager.onDispatchEvent(DeliveryCompletedEvent(
            orderId = order.id,
            driverId = driverId
        ))

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DELIVERED, getOrderStatus(order.id, userId))
        }
    }

    @Test
    fun `delivery failed marks order as DISPATCH_FAILED`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)

        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onRestaurantEvent(RestaurantOrderAcceptedEvent(
            orderId = order.id,
            restaurantId = restaurantId
        ))

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.AWAITING_DRIVER, getOrderStatus(order.id, userId))
        }

        sagaManager.onDispatchEvent(DriverAssignedEvent(
            orderId = order.id,
            driverId = driverId
        ))

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DRIVER_ASSIGNED, getOrderStatus(order.id, userId))
        }

        sagaManager.onDispatchEvent(DriverDeliveryFailedEvent(
            orderId = order.id,
            driverId = driverId,
            reason = "Customer not available"
        ))

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertEquals(OrderStatus.DISPATCH_FAILED, getOrderStatus(order.id, userId))
        }
    }
}

