package com.example.feastly.outbox

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(OutboxPublisher::class.java)

    companion object {
        private const val TOPIC = "order.events"
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    fun publishPendingEvents() {
        val pendingEntries = outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc()

        for (entry in pendingEntries) {
            try {
                kafkaTemplate.send(TOPIC, entry.id.toString(), entry.payload).get()
                entry.publishedAt = Instant.now()
                outboxRepository.save(entry)
                log.debug("Published outbox entry {} of type {}", entry.id, entry.eventType)
            } catch (e: Exception) {
                log.warn("Failed to publish outbox entry {}: {}", entry.id, e.message)
            }
        }
    }
}
