package com.example.feastly.outbox

import com.example.feastly.BaseIntegrationTest
import com.example.feastly.TestRestaurantMenuClient
import com.example.feastly.events.KafkaTopics
import com.example.feastly.events.OrderPlacedEvent
import com.example.feastly.events.RestaurantOrderAcceptedEvent
import com.example.feastly.order.CreateOrderRequest
import com.example.feastly.order.OrderItemRequest
import com.example.feastly.order.OrderResponse
import com.example.feastly.saga.OrderSagaManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OutboxIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaManager: OrderSagaManager

    private val objectMapper = jacksonObjectMapper()

    private fun url(path: String) = "http://localhost:$port$path"

    @BeforeEach
    fun setUp() {
        TestRestaurantMenuClient.clearMenuItems()
        outboxRepository.deleteAll()
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

    @Test
    fun `saga creates outbox entry for OrderPlacedEvent when status is SUBMITTED`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)
        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onOrderEvent(OrderPlacedEvent(
            orderId = order.id,
            userId = userId,
            totalCents = 1000
        ))

        val entries = outboxRepository.findAll()
            .filter { it.destinationTopic == KafkaTopics.RESTAURANT_ORDER_REQUEST }
            .filter { it.aggregateId == order.id }

        assertTrue(entries.isNotEmpty(), "Expected at least 1 RestaurantOrderRequest entry")
    }

    @Test
    fun `saga creates exactly one outbox entry per RestaurantOrderAcceptedEvent`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)
        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onRestaurantEvent(RestaurantOrderAcceptedEvent(
            orderId = order.id,
            restaurantId = restaurantId
        ))

        sagaManager.onRestaurantEvent(RestaurantOrderAcceptedEvent(
            orderId = order.id,
            restaurantId = restaurantId
        ))

        val entries = outboxRepository.findAll()
            .filter { it.destinationTopic == KafkaTopics.DISPATCH_ASSIGN_DRIVER }
            .filter { it.aggregateId == order.id }

        assertEquals(1, entries.size, "Expected exactly 1 AssignDriverCommand entry, but found ${entries.size}")
    }

    @Test
    fun `outbox payload is valid JSON not double-encoded`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)
        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onOrderEvent(OrderPlacedEvent(
            orderId = order.id,
            userId = userId,
            totalCents = 1000
        ))

        val entry = outboxRepository.findAll()
            .first { it.destinationTopic == KafkaTopics.RESTAURANT_ORDER_REQUEST }

        val parsed = objectMapper.readValue<Map<String, Any>>(entry.payload)

        assertTrue(parsed is Map<*, *>, "Payload should parse to Map, not String (would indicate double-encoding)")
        assertNotNull(parsed["orderId"], "Parsed payload must contain orderId")
        assertNotNull(parsed["restaurantId"], "Parsed payload must contain restaurantId")
    }

    @Test
    fun `RestaurantOrderRequest routed to correct topic`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)
        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onOrderEvent(OrderPlacedEvent(
            orderId = order.id,
            userId = userId,
            totalCents = 1000
        ))

        val entry = outboxRepository.findAll()
            .first { it.aggregateId == order.id && it.eventType == "RestaurantOrderRequest" }

        assertEquals(KafkaTopics.RESTAURANT_ORDER_REQUEST, entry.destinationTopic)
    }

    @Test
    fun `AssignDriverCommand routed to correct topic`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)
        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onRestaurantEvent(RestaurantOrderAcceptedEvent(
            orderId = order.id,
            restaurantId = restaurantId
        ))

        val entry = outboxRepository.findAll()
            .first { it.aggregateId == order.id && it.eventType == "AssignDriverCommand" }

        assertEquals(KafkaTopics.DISPATCH_ASSIGN_DRIVER, entry.destinationTopic)
    }

    @Test
    fun `outbox payload contains required command fields`() {
        val userId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()
        val menuItemId = createMenuItem(restaurantId)
        val order = createOrder(userId, restaurantId, menuItemId)

        sagaManager.onRestaurantEvent(RestaurantOrderAcceptedEvent(
            orderId = order.id,
            restaurantId = restaurantId
        ))

        val entry = outboxRepository.findAll()
            .first { it.eventType == "AssignDriverCommand" }

        val parsed = objectMapper.readValue<Map<String, Any>>(entry.payload)

        assertEquals(order.id.toString(), parsed["orderId"], "orderId must match")
        assertEquals(restaurantId.toString(), parsed["restaurantId"], "restaurantId must match")
        assertNotNull(parsed["timestamp"], "timestamp must be present")
    }
}
