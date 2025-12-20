package com.example.feastly.order

import com.example.feastly.client.RestaurantClient
import com.example.feastly.client.RestaurantMenuClient
import com.example.feastly.client.UserClient
import com.feastly.events.OrderAcceptedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.kafka.core.KafkaTemplate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceKafkaTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Test
    fun `acceptOrder publishes OrderAcceptedEvent to Kafka`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        val order = DeliveryOrder(
            customerId = customerId,
            restaurantId = restaurantId,
            status = OrderStatus.SUBMITTED.name
        )

        val idField = DeliveryOrder::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(order, orderId)

        whenever(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        whenever(orderRepository.save(any<DeliveryOrder>())).thenAnswer { it.arguments[0] }

        val orderService = createMinimalOrderService()

        orderService.acceptOrder(restaurantId, orderId)

        verify(kafkaTemplate).send(
            eq("order.events"),
            eq(orderId.toString()),
            argThat<OrderAcceptedEvent> { event ->
                event.orderId == orderId && event.restaurantId == restaurantId
            }
        )
    }

    private fun createMinimalOrderService(): OrderService {
        return OrderService(
            orderRepository = orderRepository,
            orderItemRepository = org.mockito.Mockito.mock(OrderItemRepository::class.java),
            userClient = org.mockito.Mockito.mock(UserClient::class.java),
            restaurantClient = org.mockito.Mockito.mock(RestaurantClient::class.java),
            restaurantMenuClient = org.mockito.Mockito.mock(RestaurantMenuClient::class.java),
            restaurantAvailabilityClient = org.mockito.Mockito.mock(),
            paymentService = org.mockito.Mockito.mock(),
            pricingService = org.mockito.Mockito.mock(),
            outboxRepository = org.mockito.Mockito.mock(),
            orderEventFactory = org.mockito.Mockito.mock(),
            kafkaTemplate = kafkaTemplate,
            meterRegistry = org.mockito.Mockito.mock()
        )
    }
}
