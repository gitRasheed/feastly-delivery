package com.feastly.dispatch

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
 * TODO: Add OpenTelemetry trace context propagation via Kafka headers
 */
@Component
class DispatchEventListener(
    private val dispatchService: DispatchService,
    private val dispatchAttemptRepository: DispatchAttemptRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(DispatchEventListener::class.java)

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"
    }

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = "dispatch",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderAccepted(record: ConsumerRecord<String, OrderAcceptedEvent>) {
        val event = record.value()
        val traceId = extractTraceId(record) ?: UUID.randomUUID().toString().take(8)

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            
            logger.info("Consumed OrderAcceptedEvent for order ${event.orderId}")

            // Idempotency check: skip if dispatch already in progress or completed
            if (hasExistingDispatch(event.orderId)) {
                logger.info("Skipping duplicate event - dispatch already exists for order ${event.orderId}")
                return
            }

            dispatchService.startDispatch(event.orderId)
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }

    /**
     * Extract traceId from Kafka headers if present.
     */
    private fun extractTraceId(record: ConsumerRecord<String, OrderAcceptedEvent>): String? {
        return record.headers().lastHeader(TRACE_ID_HEADER)?.value()?.let { String(it) }
    }

    /**
     * Check if a dispatch attempt already exists for this order.
     * Returns true if there's an active (PENDING/ACCEPTED) attempt.
     */
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
        groupId = "dispatch",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleAssignDriver(record: ConsumerRecord<String, AssignDriverCommand>) {
        val command = record.value()
        val traceId = record.headers().lastHeader(TRACE_ID_HEADER)?.value()?.let { String(it) }
            ?: UUID.randomUUID().toString().take(8)

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            logger.info("Received AssignDriverCommand for order ${command.orderId}")


            val driverId = UUID.randomUUID()

            val event = DriverAssignedEvent(
                orderId = command.orderId,
                driverId = driverId
            )
            kafkaTemplate.send(KafkaTopics.DISPATCH_EVENTS, command.orderId.toString(), event)
            logger.info("Emitted DriverAssignedEvent for order ${command.orderId} with driver $driverId")
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("DispatchEventListener shutting down - completing in-flight processing...")
    }
}
