package com.feastly.events

/**
 * Kafka topic names used across services.
 */
object KafkaTopics {
    const val ORDER_EVENTS = "order.events"
    const val DRIVER_LOCATIONS = "driver.locations"
    const val RESTAURANT_EVENTS = "restaurant.events"
    const val RESTAURANT_ORDER_REQUEST = "restaurant.order.request"
    const val DISPATCH_ASSIGN_DRIVER = "dispatch.assign.driver"
    const val DISPATCH_EVENTS = "dispatch.events"
}
