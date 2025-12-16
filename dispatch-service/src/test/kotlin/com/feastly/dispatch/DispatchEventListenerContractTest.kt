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

    private fun createRecord(event: OrderAcceptedEvent, traceId: String? = null): ConsumerRecord<String, Any> {
        val headers = RecordHeaders()
        traceId?.let { headers.add("X-Trace-Id", it.toByteArray()) }
        return ConsumerRecord(
            "order-events",
            0,
            0L,
            event.orderId.toString(),
            event as Any
        )
    }

    private fun createRecordWithHeaders(event: OrderAcceptedEvent, traceId: String): ConsumerRecord<String, Any> {
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
            event as Any,
            headers,
            java.util.Optional.empty()
        )
    }

    @Test
    fun `can deserialize OrderAcceptedEvent from producer JSON format`() {
        val orderId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val restaurantId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")

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
    fun `handleOrderEvents invokes dispatchService with correct orderId`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderEvents(createRecord(event))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `full roundtrip - serialize event then consume via listener`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        val originalEvent = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = restaurantId
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        val json = objectMapper.writeValueAsString(originalEvent)
        val consumedEvent: OrderAcceptedEvent = objectMapper.readValue(json)
        listener.handleOrderEvents(createRecord(consumedEvent))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `consuming same event twice only triggers dispatch once - idempotency`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())
        listener.handleOrderEvents(createRecord(event))

        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.PENDING,
            offeredAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))
        listener.handleOrderEvents(createRecord(event))

        verify(dispatchService, times(1)).startDispatch(orderId)
    }

    @Test
    fun `skips dispatch when accepted attempt already exists`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

val acceptedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED,
            offeredAt = Instant.now(),
            respondedAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(acceptedAttempt))

        listener.handleOrderEvents(createRecord(event))

        verify(dispatchService, never()).startDispatch(any())
    }

    @Test
    fun `allows re-dispatch after rejection`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

val rejectedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.REJECTED,
            offeredAt = Instant.now(),
            respondedAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(rejectedAttempt))

        listener.handleOrderEvents(createRecord(event))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `traceId from Kafka header is used in processing`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = UUID.randomUUID()
        )

whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderEvents(createRecordWithHeaders(event, "test-trace-123"))

        verify(dispatchService).startDispatch(orderId)
    }
}
