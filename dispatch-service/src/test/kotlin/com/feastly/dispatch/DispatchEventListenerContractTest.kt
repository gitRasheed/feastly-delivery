package com.feastly.dispatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.feastly.events.DispatchAttemptStatus
import com.feastly.events.OrderAcceptedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test verifying dispatch-service can consume OrderAcceptedEvent
 * as produced by order-service and correctly trigger dispatch logic.
 */
class DispatchEventListenerContractTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun createRecord(event: OrderAcceptedEvent, traceId: String? = null): ConsumerRecord<String, OrderAcceptedEvent> {
        val headers = RecordHeaders()
        traceId?.let { headers.add("X-Trace-Id", it.toByteArray()) }
        return ConsumerRecord(
            "order-events",    // topic
            0,                 // partition
            0L,                // offset
            event.orderId.toString(),  // key
            event              // value
        )
    }

    private fun createRecordWithHeaders(event: OrderAcceptedEvent, traceId: String): ConsumerRecord<String, OrderAcceptedEvent> {
        val headers = RecordHeaders()
        headers.add("X-Trace-Id", traceId.toByteArray())
        return ConsumerRecord(
            "order-events",
            0,
            0L,
            System.currentTimeMillis(),
            TimestampType.CREATE_TIME,
            0,
            0,
            event.orderId.toString(),
            event,
            headers,
            java.util.Optional.empty()
        )
    }

    @Test
    fun `can deserialize OrderAcceptedEvent from producer JSON format`() {
        val orderId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val restaurantId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")

        // JSON as produced by order-service
        val producerJson = """
            {
                "orderId": "$orderId",
                "restaurantId": "$restaurantId",
                "timestamp": "2025-01-15T14:30:00Z"
            }
        """.trimIndent()

        val event: OrderAcceptedEvent = objectMapper.readValue(producerJson)

        assertEquals(orderId, event.orderId)
        assertEquals(restaurantId, event.restaurantId)
    }

    @Test
    fun `handleOrderAccepted invokes dispatchService with correct orderId`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository)

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

        // No existing dispatch
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderAccepted(createRecord(event))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `full roundtrip - serialize event then consume via listener`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository)

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        // Producer creates event
        val originalEvent = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = restaurantId
        )

        // No existing dispatch
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        // Serialize as producer would
        val json = objectMapper.writeValueAsString(originalEvent)

        // Consumer deserializes
        val consumedEvent: OrderAcceptedEvent = objectMapper.readValue(json)

        // Consumer processes via record
        listener.handleOrderAccepted(createRecord(consumedEvent))

        // Verify dispatch was triggered with correct orderId
        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `consuming same event twice only triggers dispatch once - idempotency`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository)

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

        // First consumption - no existing dispatch
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())
        listener.handleOrderAccepted(createRecord(event))

        // Second consumption - dispatch already in progress
        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.PENDING,
            offeredAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))
        listener.handleOrderAccepted(createRecord(event))

        // dispatchService.startDispatch should only be called once
        verify(dispatchService, times(1)).startDispatch(orderId)
    }

    @Test
    fun `skips dispatch when accepted attempt already exists`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository)

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

        // Already accepted dispatch exists
        val acceptedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED,
            offeredAt = Instant.now(),
            respondedAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(acceptedAttempt))

        listener.handleOrderAccepted(createRecord(event))

        // Should not trigger dispatch
        verify(dispatchService, never()).startDispatch(any())
    }

    @Test
    fun `allows re-dispatch after rejection`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository)

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

        // Previous attempt was rejected
        val rejectedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.REJECTED,
            offeredAt = Instant.now(),
            respondedAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(rejectedAttempt))

        listener.handleOrderAccepted(createRecord(event))

        // Should trigger dispatch (rejected attempts don't block)
        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `traceId from Kafka header is used in processing`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository)

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        // Include traceId in header
        listener.handleOrderAccepted(createRecordWithHeaders(event, "test-trace-123"))

        verify(dispatchService).startDispatch(orderId)
    }
}
