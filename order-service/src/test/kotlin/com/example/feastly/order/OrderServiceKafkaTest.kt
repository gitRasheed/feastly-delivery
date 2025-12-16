package com.example.feastly.order

import com.example.feastly.payment.PaymentStatus
import com.example.feastly.restaurant.Restaurant
import com.example.feastly.user.User
import com.feastly.events.OrderAcceptedEvent
import org.junit.jupiter.api.BeforeEach
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

/**
 * Unit tests to verify OrderService publishes Kafka events correctly.
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceKafkaTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private lateinit var testUser: User
    private lateinit var testRestaurant: Restaurant

    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "test@test.com",
            password = "hashedpassword"
        )

        testRestaurant = Restaurant(
            name = "Test Restaurant",
            address = "123 Test St",
            cuisine = "Italian"
        )
    }

    @Test
    fun `acceptOrder publishes OrderAcceptedEvent to Kafka`() {
        val orderId = UUID.randomUUID()
        val restaurantId = testRestaurant.id

        val order = DeliveryOrder(
            user = testUser,
            restaurant = testRestaurant,
            status = OrderStatus.SUBMITTED,
            paymentStatus = PaymentStatus.PENDING
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
            userRepository = org.mockito.Mockito.mock(),
            restaurantRepository = org.mockito.Mockito.mock(),
            menuItemRepository = org.mockito.Mockito.mock(),
            paymentService = org.mockito.Mockito.mock(),
            pricingService = org.mockito.Mockito.mock(),
            kafkaTemplate = kafkaTemplate
        )
    }
}
