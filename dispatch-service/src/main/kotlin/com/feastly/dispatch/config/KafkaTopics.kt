package com.feastly.dispatch.config

/**
 * Kafka topic names used by dispatch-service.
 */
object KafkaTopics {
    const val ORDER_EVENTS = "order.events"
    const val DISPATCH_ASSIGN_DRIVER = "dispatch.assign.driver"
    const val DISPATCH_EVENTS = "dispatch.events"
}
