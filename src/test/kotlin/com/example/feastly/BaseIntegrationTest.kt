package com.example.feastly

import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.core.KafkaTemplate

abstract class BaseIntegrationTest {

    @MockBean
    protected lateinit var kafkaTemplate: KafkaTemplate<String, Any>
}
