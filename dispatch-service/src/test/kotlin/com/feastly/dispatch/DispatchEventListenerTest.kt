package com.feastly.dispatch

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
}
