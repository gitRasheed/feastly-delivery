package com.feastly.dispatch.ports

import com.feastly.events.AvailableDriver
import com.feastly.events.OrderInfo
import com.feastly.events.OrderStatus
import java.util.UUID

/**
 * Port for querying order information from order-service.
 * Will be replaced with Kafka-based implementation.
 */
interface OrderQueryPort {
    fun getOrderInfo(orderId: UUID): OrderInfo?
    fun updateOrderDriver(orderId: UUID, driverId: UUID?)
    fun updateOrderStatus(orderId: UUID, status: OrderStatus)
}

/**
 * Port for querying driver availability from driver-tracking-service.
 * Will be replaced with Kafka-based implementation.
 */
interface DriverStatusPort {
    fun getAvailableDrivers(): List<AvailableDriver>
}
