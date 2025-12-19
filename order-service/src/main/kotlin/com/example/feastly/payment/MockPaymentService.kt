package com.example.feastly.payment

import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.order.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Primary
class MockPaymentService(
    private val orderRepository: OrderRepository
) : PaymentService {

    private val logger = LoggerFactory.getLogger(MockPaymentService::class.java)

    override fun chargeOrder(
        customerId: UUID,
        orderId: UUID,
        amountCents: Int,
        method: PaymentMethod
    ): PaymentResult {
        val providerPaymentId = "mock_${UUID.randomUUID()}"
        logger.info("Mock payment charged: order=$orderId, amount=$amountCents, method=$method, ref=$providerPaymentId")

        return PaymentResult(
            success = true,
            paymentStatus = PaymentStatus.PAID,
            providerPaymentId = providerPaymentId,
            message = "Mock payment approved"
        )
    }

    override fun refundOrder(orderId: UUID): RefundResult {
        logger.info("Mock refund processed: order=$orderId")

        return RefundResult(
            success = true,
            paymentStatus = PaymentStatus.REFUNDED,
            message = "Mock refund processed"
        )
    }

    override fun getPaymentStatusForOrder(orderId: UUID): PaymentStatus {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        return PaymentStatus.PAID
    }
}
