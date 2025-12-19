package com.example.feastly.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_outbox")
class OutboxEntry(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "published_at")
    var publishedAt: Instant? = null
) {
    val processed: Boolean get() = publishedAt != null
}
