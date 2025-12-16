package com.example.feastly.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    companion object {
        const val ORDER_EVENTS_TOPIC = "order.events"
        const val RESTAURANT_EVENTS_TOPIC = "restaurant.events"
        const val DRIVER_LOCATIONS_TOPIC = "driver.locations"

        private const val DEFAULT_PARTITIONS = 3
        private const val DEFAULT_REPLICAS = 1
    }

    @Bean
    fun orderEventsTopic(): NewTopic =
        TopicBuilder.name(ORDER_EVENTS_TOPIC)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()

    @Bean
    fun restaurantEventsTopic(): NewTopic =
        TopicBuilder.name(RESTAURANT_EVENTS_TOPIC)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()

    @Bean
    fun driverLocationsTopic(): NewTopic =
        TopicBuilder.name(DRIVER_LOCATIONS_TOPIC)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
}
