package com.feastly.dispatch

import com.feastly.events.OrderAcceptedEvent
import io.micrometer.core.instrument.MeterRegistry
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

    @Mock
    private lateinit var meterRegistry: MeterRegistry

    @InjectMocks
    private lateinit var listener: DispatchEventListener

    private fun createRecord(event: OrderAcceptedEvent): ConsumerRecord<String, Any> {
        return ConsumerRecord(
            "order-events",
            0,
            0L,
            event.orderId.toString(),
            event as Any
        )
    }

    @Test
    fun `handleOrderEvents calls DispatchService startDispatch with correct orderId`() {
        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = restaurantId,
            timestamp = Instant.now()
        )

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderEvents(createRecord(event))

        verify(dispatchService).startDispatch(orderId)
    }
}
