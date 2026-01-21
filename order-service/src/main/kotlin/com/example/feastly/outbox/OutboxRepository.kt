package com.example.feastly.outbox

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OutboxRepository : JpaRepository<OutboxEntry, UUID> {
    fun findByPublishedAtIsNullOrderByCreatedAtAsc(): List<OutboxEntry>
    fun existsByAggregateIdAndEventType(aggregateId: UUID, eventType: String): Boolean
}
