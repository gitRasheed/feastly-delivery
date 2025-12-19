package com.feastly.dispatch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.feastly.dispatch.events.OrderEventEnvelope
import com.feastly.dispatch.events.OrderEventTypes
import com.feastly.events.AssignDriverCommand
import com.feastly.events.DispatchAttemptStatus
import com.feastly.events.DriverAssignedEvent
import com.feastly.events.KafkaTopics
import com.feastly.events.OrderAcceptedEvent
import jakarta.annotation.PreDestroy
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Kafka consumer for order events.
 *
 * Handles both:
 * - ORDER_SUBMITTED events from the new envelope format (outbox)
 * - OrderAcceptedEvent typed events (legacy/direct Kafka)
 */
@Component
class DispatchEventListener(
    private val dispatchService: DispatchService,
    private val dispatchAttemptRepository: DispatchAttemptRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(DispatchEventListener::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"

        // TODO: Replace with actual driver selection in future phases
        val PLACEHOLDER_DRIVER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = "dispatch-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderEvents(record: ConsumerRecord<String, Any>) {
        val event = record.value()
        val traceId = extractTraceId(record) ?: UUID.randomUUID().toString().take(8)

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            
            when (event) {
                is String -> handleEnvelopeEvent(event)
                is OrderAcceptedEvent -> handleOrderAccepted(event)
                else -> logger.debug("Ignoring event type: ${event::class.simpleName}")
            }
        } catch (e: Exception) {
            logger.error("Error processing event: ${e.message}", e)
            throw e
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }

    private fun handleEnvelopeEvent(jsonPayload: String) {
        val envelope = try {
            objectMapper.readValue(jsonPayload, OrderEventEnvelope::class.java)
        } catch (e: Exception) {
            logger.debug("Not a valid envelope JSON, ignoring: ${e.message}")
            return
        }

        envelope.trace?.traceId?.let { MDC.put(TRACE_ID_MDC_KEY, it) }
        
        when (envelope.eventType) {
            OrderEventTypes.ORDER_SUBMITTED -> handleOrderSubmitted(envelope)
            OrderEventTypes.ORDER_ACCEPTED -> {
                if (!hasExistingDispatch(envelope.order.orderId)) {
                    dispatchService.startDispatch(envelope.order.orderId)
                }
            }
            else -> logger.debug("Ignoring envelope event type: ${envelope.eventType}")
        }
    }


    private fun handleOrderSubmitted(envelope: OrderEventEnvelope) {
        val orderId = envelope.order.orderId
        
        logger.info("Processing ORDER_SUBMITTED for order $orderId (eventId: ${envelope.eventId})")
        
        if (hasExistingDispatch(orderId)) {
            logger.info("Skipping duplicate ORDER_SUBMITTED - dispatch already exists for order $orderId")
            return
        }

        dispatchService.createInitialDispatchAttempt(orderId)
    }

    private fun handleOrderAccepted(event: OrderAcceptedEvent) {
        logger.info("Consumed OrderAcceptedEvent for order ${event.orderId}")
        if (!hasExistingDispatch(event.orderId)) {
            dispatchService.startDispatch(event.orderId)
        } else {
            logger.info("Skipping duplicate event - dispatch already exists for order ${event.orderId}")
        }
    }

    private fun extractTraceId(record: ConsumerRecord<String, Any>): String? {
        return record.headers().lastHeader(TRACE_ID_HEADER)?.value()?.let { String(it) }
    }

    private fun hasExistingDispatch(orderId: UUID): Boolean {
        val existingAttempts = dispatchAttemptRepository.findByOrderId(orderId)
        return existingAttempts.any { attempt ->
            attempt.status in listOf(
                DispatchAttemptStatus.PENDING,
                DispatchAttemptStatus.ACCEPTED
            )
        }
    }

    @KafkaListener(
        topics = [KafkaTopics.DISPATCH_ASSIGN_DRIVER],
        groupId = "dispatch-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleAssignDriver(record: ConsumerRecord<String, Any>) {
        val event = record.value()
        val traceId = extractTraceId(record) ?: UUID.randomUUID().toString().take(8)

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            
            when (event) {
                is AssignDriverCommand -> {
                    logger.info("Received AssignDriverCommand for order ${event.orderId}")
                    val driverId = UUID.randomUUID()
                    val assignedEvent = DriverAssignedEvent(
                        orderId = event.orderId,
                        driverId = driverId
                    )
                    kafkaTemplate.send(KafkaTopics.DISPATCH_EVENTS, event.orderId.toString(), assignedEvent)
                    logger.info("Emitted DriverAssignedEvent for order ${event.orderId} with driver $driverId")
                }
                else -> logger.warn("Unexpected event type: ${event::class.simpleName}")
            }
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("DispatchEventListener shutting down - completing in-flight processing...")
    }
}

