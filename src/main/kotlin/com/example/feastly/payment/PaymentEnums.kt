package com.example.feastly.payment

enum class PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
    FAILED
}

enum class PaymentMethod {
    MOCK,
    CARD,
    CASH_ON_DELIVERY
}
