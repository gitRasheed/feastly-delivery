package com.example.feastly.payment

data class PaymentResult(
    val success: Boolean,
    val paymentStatus: PaymentStatus,
    val providerPaymentId: String?,
    val message: String
)

data class RefundResult(
    val success: Boolean,
    val paymentStatus: PaymentStatus,
    val message: String
)
