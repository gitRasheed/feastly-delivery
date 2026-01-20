package com.example.feastly.events

/**
 * Kafka topic names used by order-service.
 */
object KafkaTopics {
    const val ORDER_EVENTS = "order.events"
    const val RESTAURANT_EVENTS = "restaurant.events"
    const val RESTAURANT_ORDER_REQUEST = "restaurant.order.request"
    const val DISPATCH_ASSIGN_DRIVER = "dispatch.assign.driver"
    const val DISPATCH_EVENTS = "dispatch.events"
}
