package com.example.feastly.config

import com.example.feastly.driverstatus.DriverLocationAggregator
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate

@TestConfiguration
class TestKafkaConfig {

    @Bean
    @Primary
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return Mockito.mock(KafkaTemplate::class.java) as KafkaTemplate<String, Any>
    }

    @Bean
    @Primary
    fun driverLocationAggregator(kafkaTemplate: KafkaTemplate<String, Any>): DriverLocationAggregator {
        return DriverLocationAggregator(kafkaTemplate)
    }
}
