package com.feastly.events

/**
 * Shared order status enum used across services.
 */
enum class OrderStatus {
    SUBMITTED,
    ACCEPTED,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}

/**
 * Dispatch attempt status for tracking driver offers.
 */
enum class DispatchAttemptStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
