package com.feastlydelivery.restaurant.listener

import com.feastlydelivery.restaurant.Restaurant
import com.feastlydelivery.restaurant.RestaurantRepository
import com.feastlydelivery.restaurant.config.KafkaTopics
import com.feastlydelivery.restaurant.events.RestaurantOrderAcceptedEvent
import com.feastlydelivery.restaurant.events.RestaurantOrderRejectedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class RestaurantEventListenerTest {

    @Mock
    private lateinit var restaurantRepository: RestaurantRepository

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @InjectMocks
    private lateinit var listener: RestaurantEventListener

    private val orderId = UUID.randomUUID()
    private val restaurantId = UUID.randomUUID()

    @Test
    fun `emits RestaurantOrderAcceptedEvent when restaurant exists and is open`() {
        val restaurant = Restaurant(
            id = restaurantId,
            ownerUserId = UUID.randomUUID(),
            name = "Test Pizza",
            isOpen = true
        )
        whenever(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant))

        val record = createRecord(orderId, restaurantId)
        listener.handleOrderRequest(record)

        val eventCaptor: ArgumentCaptor<Any> = ArgumentCaptor.forClass(Any::class.java)
        verify(kafkaTemplate).send(eq(KafkaTopics.RESTAURANT_EVENTS), eq(orderId.toString()), capture(eventCaptor))
        
        val emittedEvent = eventCaptor.value
        assertTrue(emittedEvent is RestaurantOrderAcceptedEvent)
        assertEquals(orderId, emittedEvent.orderId)
        assertEquals(restaurantId, emittedEvent.restaurantId)
    }

    @Test
    fun `emits RestaurantOrderRejectedEvent when restaurant not found`() {
        whenever(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty())

        val record = createRecord(orderId, restaurantId)
        listener.handleOrderRequest(record)

        val eventCaptor: ArgumentCaptor<Any> = ArgumentCaptor.forClass(Any::class.java)
        verify(kafkaTemplate).send(eq(KafkaTopics.RESTAURANT_EVENTS), eq(orderId.toString()), capture(eventCaptor))
        
        val emittedEvent = eventCaptor.value
        assertTrue(emittedEvent is RestaurantOrderRejectedEvent)
        assertEquals(orderId, emittedEvent.orderId)
        assertEquals("Restaurant not found", emittedEvent.reason)
    }

    @Test
    fun `emits RestaurantOrderRejectedEvent when restaurant is closed`() {
        val restaurant = Restaurant(
            id = restaurantId,
            ownerUserId = UUID.randomUUID(),
            name = "Closed Diner",
            isOpen = false
        )
        whenever(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant))

        val record = createRecord(orderId, restaurantId)
        listener.handleOrderRequest(record)

        val eventCaptor: ArgumentCaptor<Any> = ArgumentCaptor.forClass(Any::class.java)
        verify(kafkaTemplate).send(eq(KafkaTopics.RESTAURANT_EVENTS), eq(orderId.toString()), capture(eventCaptor))
        
        val emittedEvent = eventCaptor.value
        assertTrue(emittedEvent is RestaurantOrderRejectedEvent)
        assertEquals("Restaurant is closed", emittedEvent.reason)
    }

    private fun createRecord(orderId: UUID, restaurantId: UUID): ConsumerRecord<String, Any> {
        val eventMap = mapOf(
            "orderId" to orderId.toString(),
            "restaurantId" to restaurantId.toString()
        )
        return ConsumerRecord(
            KafkaTopics.RESTAURANT_ORDER_REQUEST,
            0,
            0L,
            orderId.toString(),
            eventMap as Any
        )
    }
}
