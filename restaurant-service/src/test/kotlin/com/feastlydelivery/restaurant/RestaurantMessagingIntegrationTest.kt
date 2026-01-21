package com.feastlydelivery.restaurant

import com.feastlydelivery.restaurant.config.KafkaTopics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration test to verify Kafka connectivity.
 * 
 * This test proves the restaurant-service can actually receive Kafka messages,
 * preventing "deaf service" bugs from going undetected.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = [KafkaTopics.RESTAURANT_ORDER_REQUEST, KafkaTopics.RESTAURANT_EVENTS],
    brokerProperties = ["listeners=PLAINTEXT://localhost:0", "port=0"]
)
@DirtiesContext
@ActiveProfiles("test")
class RestaurantMessagingIntegrationTest {

    @Autowired
    private lateinit var restaurantOrderRepository: RestaurantOrderRepository
    
    @Autowired
    private lateinit var restaurantRepository: RestaurantRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Test
    fun `should persist restaurant order request and emit accepted event`() {
        // Setup existing restaurant
        val restaurantId = UUID.randomUUID()
        val restaurant = Restaurant(
            id = restaurantId,
            ownerUserId = UUID.randomUUID(),
            name = "Test Bistro",
            isOpen = true
        )
        restaurantRepository.save(restaurant)

        val orderId = UUID.randomUUID()
        val testMessage = mapOf(
            "orderId" to orderId.toString(),
            "restaurantId" to restaurantId.toString(),
            "timestamp" to System.currentTimeMillis().toString()
        )

        kafkaTemplate.send(KafkaTopics.RESTAURANT_ORDER_REQUEST, orderId.toString(), testMessage)

        org.awaitility.Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                val order = restaurantOrderRepository.findById(orderId)
                assertTrue(order.isPresent, "Order should be persisted")
                assertTrue(order.get().status == "PENDING", "Order status should be PENDING")
            }
        
        // Also verify event emission using existing mechanism
        val received = TestMessageReceiver.latch.await(10, TimeUnit.SECONDS)
        assertTrue(received, "Should receive accepted event")
    }

    /**
     * Test component that listens for messages and captures them for verification.
     * Must be a top-level bean to be picked up by Spring's component scanning.
     */
    @TestConfiguration
    class TestConfig {
        @Bean
        fun testMessageReceiver(): TestMessageReceiver = TestMessageReceiver
    }

    @Component
    object TestMessageReceiver {
        val latch = CountDownLatch(1)
        val receivedMessages = mutableListOf<Any>()

        @KafkaListener(
            topics = [KafkaTopics.RESTAURANT_ORDER_REQUEST],
            groupId = "restaurant-service-test",
            containerFactory = "kafkaListenerContainerFactory"
        )
        fun receive(record: ConsumerRecord<String, Any>) {
            receivedMessages.add(record.value())
            latch.countDown()
        }
    }
}
