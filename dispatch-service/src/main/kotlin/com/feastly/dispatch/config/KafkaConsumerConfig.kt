package com.feastly.dispatch.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConsumerConfig(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String
) {
    private val logger = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

    companion object {
        private const val MAX_POLL_INTERVAL_MS = 300_000
        private const val MAX_POLL_RECORDS = 10
        private const val RETRY_INTERVAL_MS = 1000L
        private const val MAX_RETRIES = 3L
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val jsonDeserializer = JsonDeserializer(Any::class.java).apply {
            addTrustedPackages("com.feastly.*")
            setUseTypeHeaders(true)
        }

        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "dispatch",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to MAX_POLL_RECORDS
        )
        return DefaultKafkaConsumerFactory(configProps, StringDeserializer(), jsonDeserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()

        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD

        val errorHandler = DefaultErrorHandler(
            { record, exception ->
                logger.error("Failed to process record after retries: ${record.value()}", exception)
            },
            FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES)
        )
        factory.setCommonErrorHandler(errorHandler)

        return factory
    }
}
