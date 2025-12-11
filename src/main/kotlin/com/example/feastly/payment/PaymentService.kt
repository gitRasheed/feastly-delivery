package com.example.feastly.payment

import java.util.UUID

interface PaymentService {

    fun chargeOrder(
        customerId: UUID,
        orderId: UUID,
        amountCents: Int,
        method: PaymentMethod = PaymentMethod.MOCK
    ): PaymentResult

    fun refundOrder(orderId: UUID): RefundResult

    fun getPaymentStatusForOrder(orderId: UUID): PaymentStatus
}
