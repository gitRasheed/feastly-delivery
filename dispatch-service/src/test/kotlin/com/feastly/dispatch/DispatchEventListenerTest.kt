package com.feastly.dispatch

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.feastly.dispatch.events.DispatchAttemptStatus
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DispatchEventListenerTest {

    @Mock
    private lateinit var dispatchService: DispatchService

    @Mock
    private lateinit var dispatchAttemptRepository: DispatchAttemptRepository

    @Mock
    private lateinit var kafkaTemplate: org.springframework.kafka.core.KafkaTemplate<String, Any>

    @InjectMocks
    private lateinit var listener: DispatchEventListener

    private fun createMapRecord(orderId: UUID, restaurantId: UUID): ConsumerRecord<String, Any> {
        val eventMap = mapOf(
            "orderId" to orderId.toString(),
            "restaurantId" to restaurantId.toString(),
            "timestamp" to Instant.now().toString()
        )
        return ConsumerRecord(
            "order-events",
            0,
            0L,
            orderId.toString(),
            eventMap as Any
        )
    }

    @Test
    fun `handleOrderEvents calls DispatchService startDispatch with correct orderId`() {
        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `onRestaurantOrderAccepted_shouldBeIdempotent`() {
        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        // First call: No active dispatch
        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(emptyList())

        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))
        
        // Second call: Active dispatch exists (simulating state change)
        val activeAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(listOf(activeAttempt))

        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        // Assert: startDispatch called only once
        verify(dispatchService, org.mockito.kotlin.times(1)).startDispatch(orderId)
    }
}
