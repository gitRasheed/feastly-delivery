package com.feastly.dispatch

import com.feastly.events.KafkaTopics
import com.feastly.events.OrderAcceptedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class DispatchEventListener(
    private val dispatchService: DispatchService
) {
    private val logger = LoggerFactory.getLogger(DispatchEventListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = "dispatch",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderAccepted(event: OrderAcceptedEvent) {
        logger.info("Consumed OrderAcceptedEvent: dispatching order ${event.orderId}")
        dispatchService.startDispatch(event.orderId)
    }
}
