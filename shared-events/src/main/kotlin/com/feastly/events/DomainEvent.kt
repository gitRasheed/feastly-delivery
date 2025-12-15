package com.feastly.events

import java.time.Instant

/**
 * Marker interface for all domain events.
 */
interface DomainEvent {
    val timestamp: Instant
}
