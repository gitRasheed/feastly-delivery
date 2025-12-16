package com.feastly.dispatch.config

import com.feastly.events.KafkaTopics
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    companion object {
        private const val DEFAULT_PARTITIONS = 3
        private const val DEFAULT_REPLICAS = 1
    }

    @Bean
    fun orderEventsTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.ORDER_EVENTS)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
}
