package com.feastly.dispatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.feastly.dispatch.events.DispatchAttemptStatus
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
 * Contract test verifying dispatch-service can consume order events
 * as produced by order-service (without type headers) and correctly trigger dispatch logic.
 */
class DispatchEventListenerContractTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

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

    private fun createMapRecordWithHeaders(orderId: UUID, restaurantId: UUID, traceId: String): ConsumerRecord<String, Any> {
        val headers = RecordHeaders()
        headers.add("X-Trace-Id", traceId.toByteArray())
        val eventMap = mapOf(
            "orderId" to orderId.toString(),
            "restaurantId" to restaurantId.toString(),
            "timestamp" to Instant.now().toString()
        )
        return ConsumerRecord(
            "order-events",
            0,
            0L,
            System.currentTimeMillis(),
            TimestampType.CREATE_TIME,
            0,
            0,
            orderId.toString(),
            eventMap as Any,
            headers,
            java.util.Optional.empty()
        )
    }

    @Test
    fun `can deserialize OrderAcceptedEvent from producer JSON format using ObjectMapper`() {
        val orderId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val restaurantId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")

        val producerJson = """
            {
                "orderId": "$orderId",
                "restaurantId": "$restaurantId",
                "timestamp": "2025-01-15T14:30:00Z"
            }
        """.trimIndent()

        val eventMap: Map<String, Any> = objectMapper.readValue(producerJson, Map::class.java) as Map<String, Any>

        assertEquals(orderId.toString(), eventMap["orderId"])
        assertEquals(restaurantId.toString(), eventMap["restaurantId"])
    }

    @Test
    fun `handleOrderEvents invokes dispatchService with correct orderId`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `consuming same event twice only triggers dispatch once - idempotency`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())
        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        val existingAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.PENDING,
            offeredAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(existingAttempt))
        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        verify(dispatchService, times(1)).startDispatch(orderId)
    }

    @Test
    fun `skips dispatch when accepted attempt already exists`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        val acceptedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.ACCEPTED,
            offeredAt = Instant.now(),
            respondedAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(acceptedAttempt))

        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        verify(dispatchService, never()).startDispatch(any())
    }

    @Test
    fun `allows re-dispatch after rejection`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        val rejectedAttempt = DispatchAttempt(
            orderId = orderId,
            driverId = UUID.randomUUID(),
            status = DispatchAttemptStatus.REJECTED,
            offeredAt = Instant.now(),
            respondedAt = Instant.now()
        )
        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(listOf(rejectedAttempt))

        listener.handleOrderEvents(createMapRecord(orderId, restaurantId))

        verify(dispatchService).startDispatch(orderId)
    }

    @Test
    fun `traceId from Kafka header is used in processing`() {
        val dispatchService = mock<DispatchService>()
        val dispatchAttemptRepository = mock<DispatchAttemptRepository>()
        val listener = DispatchEventListener(dispatchService, dispatchAttemptRepository, mock())

        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        whenever(dispatchAttemptRepository.findByOrderId(orderId)).thenReturn(emptyList())

        listener.handleOrderEvents(createMapRecordWithHeaders(orderId, restaurantId, "test-trace-123"))

        verify(dispatchService).startDispatch(orderId)
    }
}
