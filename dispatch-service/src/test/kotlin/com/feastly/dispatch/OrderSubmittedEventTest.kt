package com.feastly.dispatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.feastly.dispatch.events.OrderEventEnvelope
import com.feastly.dispatch.events.OrderEventTypes
import com.feastly.dispatch.events.OrderItemSnapshot
import com.feastly.dispatch.events.OrderSnapshot
import com.feastly.dispatch.events.PricingSnapshot
import com.feastly.dispatch.events.TraceContext
import com.feastly.events.DispatchAttemptStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

/**
 * Unit tests for ORDER_SUBMITTED event handling in DispatchEventListener.
 * Tests idempotency and event filtering.
 */
@ExtendWith(MockitoExtension::class)
class OrderSubmittedEventTest {

    @Mock
    private lateinit var dispatchService: DispatchService

    @Mock
    private lateinit var dispatchAttemptRepository: DispatchAttemptRepository

    @Mock
    private lateinit var kafkaTemplate: org.springframework.kafka.core.KafkaTemplate<String, Any>

    @InjectMocks
    private lateinit var listener: DispatchEventListener

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    private fun createEnvelopeRecord(envelope: OrderEventEnvelope): ConsumerRecord<String, Any> {
        val json = objectMapper.writeValueAsString(envelope)
        return ConsumerRecord(
            "order.events",
            0,
            0L,
            envelope.order.orderId.toString(),
            json as Any
        )
    }

    private fun createOrderSubmittedEnvelope(orderId: UUID = UUID.randomUUID()): OrderEventEnvelope {
        return OrderEventEnvelope(
            eventId = UUID.randomUUID(),
            eventType = OrderEventTypes.ORDER_SUBMITTED,
            occurredAt = Instant.now(),
            trace = TraceContext("trace-123", "span-456"),
            order = OrderSnapshot(
                orderId = orderId,
                customerId = UUID.randomUUID(),
                restaurantId = UUID.randomUUID(),
                status = "SUBMITTED",
                pricing = PricingSnapshot(
                    subtotalCents = 3000,
                    taxCents = 300,
                    deliveryFeeCents = 299,
                    totalCents = 3599
                ),
                items = listOf(
                    OrderItemSnapshot(
                        menuItemId = UUID.randomUUID(),
                        menuItemName = "Test Pizza",
                        quantity = 2,
                        priceCents = 1500
                    )
                ),
                createdAt = Instant.now()
            )
        )
    }

    @Test
    fun `ORDER_SUBMITTED creates dispatch attempt when none exists`() {
        // Given
        val envelope = createOrderSubmittedEnvelope()
        val orderId = envelope.order.orderId
        
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        // When
        listener.handleOrderEvents(createEnvelopeRecord(envelope))

        // Then - service method should be called
        verify(dispatchService).createInitialDispatchAttempt(orderId)
    }

    @Test
    fun `ORDER_SUBMITTED with existing PENDING attempt does not create duplicate`() {
        // Given
        val orderId = UUID.randomUUID()
        val envelope = createOrderSubmittedEnvelope(orderId)
        
        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.PENDING
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))

        // When
        listener.handleOrderEvents(createEnvelopeRecord(envelope))

        // Then - service method should NOT be called
        verify(dispatchService, never()).createInitialDispatchAttempt(any())
    }

    @Test
    fun `ORDER_SUBMITTED with existing ACCEPTED attempt does not create duplicate`() {
        // Given
        val orderId = UUID.randomUUID()
        val envelope = createOrderSubmittedEnvelope(orderId)
        
        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))

        // When
        listener.handleOrderEvents(createEnvelopeRecord(envelope))

        // Then - service method should NOT be called
        verify(dispatchService, never()).createInitialDispatchAttempt(any())
    }

    @Test
    fun `non-ORDER_SUBMITTED event does not create dispatch attempt`() {
        // Given
        val envelope = createOrderSubmittedEnvelope().copy(eventType = "ORDER_CANCELLED")
        
        // When
        listener.handleOrderEvents(createEnvelopeRecord(envelope))

        // Then - service method should NOT be called for unhandled event types
        verify(dispatchService, never()).createInitialDispatchAttempt(any())
    }

    @Test
    fun `same ORDER_SUBMITTED event processed twice only creates one attempt`() {
        // Given
        val envelope = createOrderSubmittedEnvelope()
        val orderId = envelope.order.orderId
        
        // First call: no existing attempts, second call: has PENDING attempt
        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(emptyList())
            .thenReturn(listOf(
                DispatchAttempt(
                    orderId = orderId,
                    driverId = DispatchService.PLACEHOLDER_DRIVER_ID,
                    status = DispatchAttemptStatus.PENDING
                )
            ))

        // When - process same event twice
        listener.handleOrderEvents(createEnvelopeRecord(envelope))
        listener.handleOrderEvents(createEnvelopeRecord(envelope))

        // Then - service method should only be called once
        verify(dispatchService, org.mockito.kotlin.times(1)).createInitialDispatchAttempt(orderId)
    }
}

