package com.example.feastly.outbox

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    @Qualifier("outboxKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(OutboxPublisher::class.java)

    @Scheduled(fixedRate = 1000)
    @Transactional
    fun publishPendingEvents() {
        val pendingEntries = outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc()

        for (entry in pendingEntries) {
            try {
                kafkaTemplate.send(entry.destinationTopic, entry.id.toString(), entry.payload).get()
                entry.publishedAt = Instant.now()
                outboxRepository.save(entry)
                log.debug("Published outbox entry {} to topic {}", entry.id, entry.destinationTopic)
            } catch (e: Exception) {
                log.warn("Failed to publish outbox entry {} to topic {}: {}", entry.id, entry.destinationTopic, e.message)
            }
        }
    }
}
