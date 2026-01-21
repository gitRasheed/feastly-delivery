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
import com.feastly.dispatch.events.DispatchAttemptStatus
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

        val envelope = createOrderSubmittedEnvelope()
        val orderId = envelope.order.orderId
        
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())


        listener.handleOrderEvents(createEnvelopeRecord(envelope))


        verify(dispatchService).createInitialDispatchAttempt(orderId)
    }

    @Test
    fun `ORDER_SUBMITTED with existing PENDING attempt does not create duplicate`() {

        val orderId = UUID.randomUUID()
        val envelope = createOrderSubmittedEnvelope(orderId)
        
        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.PENDING
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))


        listener.handleOrderEvents(createEnvelopeRecord(envelope))


        verify(dispatchService, never()).createInitialDispatchAttempt(any())
    }

    @Test
    fun `ORDER_SUBMITTED with existing ACCEPTED attempt does not create duplicate`() {

        val orderId = UUID.randomUUID()
        val envelope = createOrderSubmittedEnvelope(orderId)
        
        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))


        listener.handleOrderEvents(createEnvelopeRecord(envelope))


        verify(dispatchService, never()).createInitialDispatchAttempt(any())
    }

    @Test
    fun `non-ORDER_SUBMITTED event does not create dispatch attempt`() {

        val envelope = createOrderSubmittedEnvelope().copy(eventType = "ORDER_CANCELLED")
        

        listener.handleOrderEvents(createEnvelopeRecord(envelope))


        verify(dispatchService, never()).createInitialDispatchAttempt(any())
    }

    @Test
    fun `same ORDER_SUBMITTED event processed twice only creates one attempt`() {

        val envelope = createOrderSubmittedEnvelope()
        val orderId = envelope.order.orderId
        


        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(emptyList())
            .thenReturn(listOf(
                DispatchAttempt(
                    orderId = orderId,
                    driverId = DispatchService.PLACEHOLDER_DRIVER_ID,
                    status = DispatchAttemptStatus.PENDING
                )
            ))


        listener.handleOrderEvents(createEnvelopeRecord(envelope))
        listener.handleOrderEvents(createEnvelopeRecord(envelope))


        verify(dispatchService, org.mockito.kotlin.times(1)).createInitialDispatchAttempt(orderId)
    }

    @Test
    fun `ORDER_ACCEPTED starts dispatch when only placeholder attempt exists`() {

        val orderId = UUID.randomUUID()
        val acceptedEnvelope = createOrderSubmittedEnvelope(orderId)
            .copy(eventType = OrderEventTypes.ORDER_ACCEPTED)
        

        val placeholderAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = DispatchService.PLACEHOLDER_DRIVER_ID,
            status = DispatchAttemptStatus.PENDING
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(listOf(placeholderAttempt))


        listener.handleOrderEvents(createEnvelopeRecord(acceptedEnvelope))


        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `ORDER_ACCEPTED is blocked when real PENDING dispatch exists`() {

        val orderId = UUID.randomUUID()
        val acceptedEnvelope = createOrderSubmittedEnvelope(orderId)
            .copy(eventType = OrderEventTypes.ORDER_ACCEPTED)
        

        val realAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(), // Real driver, not placeholder
            status = DispatchAttemptStatus.PENDING
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(listOf(realAttempt))


        listener.handleOrderEvents(createEnvelopeRecord(acceptedEnvelope))


        verify(dispatchService, never()).startDispatch(any())
    }

    @Test
    fun `ORDER_ACCEPTED is blocked when ACCEPTED dispatch exists`() {

        val orderId = UUID.randomUUID()
        val acceptedEnvelope = createOrderSubmittedEnvelope(orderId)
            .copy(eventType = OrderEventTypes.ORDER_ACCEPTED)
        

        val acceptedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(listOf(acceptedAttempt))


        listener.handleOrderEvents(createEnvelopeRecord(acceptedEnvelope))


        verify(dispatchService, never()).startDispatch(any())
    }

    @Test
    fun `duplicate ORDER_ACCEPTED does not trigger multiple dispatches`() {

        val orderId = UUID.randomUUID()
        val acceptedEnvelope = createOrderSubmittedEnvelope(orderId)
            .copy(eventType = OrderEventTypes.ORDER_ACCEPTED)
        


        whenever(dispatchAttemptRepository.findByOrderId(orderId))
            .thenReturn(listOf(
                DispatchAttempt(
                    orderId = orderId,
                    driverId = DispatchService.PLACEHOLDER_DRIVER_ID,
                    status = DispatchAttemptStatus.PENDING
                )
            ))
            .thenReturn(listOf(
                DispatchAttempt(
                    orderId = orderId,
                    driverId = UUID.randomUUID(), // Now has real driver
                    status = DispatchAttemptStatus.PENDING
                )
            ))


        listener.handleOrderEvents(createEnvelopeRecord(acceptedEnvelope))
        listener.handleOrderEvents(createEnvelopeRecord(acceptedEnvelope))


        verify(dispatchService, org.mockito.kotlin.times(1)).startDispatch(orderId)
    }
}
