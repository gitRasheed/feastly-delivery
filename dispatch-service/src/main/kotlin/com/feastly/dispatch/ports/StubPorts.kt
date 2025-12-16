package com.feastly.dispatch.ports

import com.feastly.events.AvailableDriver
import com.feastly.events.OrderInfo
import com.feastly.events.OrderStatus
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Stub implementation of OrderQueryPort for testing.
 * Will be replaced with Kafka command/query implementation.
 */
@Component
class StubOrderQueryPort : OrderQueryPort {

    // In-memory store for testing
    private val orders = ConcurrentHashMap<UUID, OrderInfo>()

    override fun getOrderInfo(orderId: UUID): OrderInfo? {
        return orders[orderId]
    }

    override fun updateOrderDriver(orderId: UUID, driverId: UUID?) {
        orders.computeIfPresent(orderId) { _, order ->
            order.copy(assignedDriverId = driverId)
        }
    }

    override fun updateOrderStatus(orderId: UUID, status: OrderStatus) {
        orders.computeIfPresent(orderId) { _, order ->
            order.copy(status = status)
        }
    }

    // Test helper
    fun addOrder(order: OrderInfo) {
        orders[order.orderId] = order
    }
}

/**
 * Stub implementation of DriverStatusPort for testing.
 * Will be replaced with Kafka query implementation.
 */
@Component
class StubDriverStatusPort : DriverStatusPort {

    private val availableDrivers = mutableListOf<AvailableDriver>()

    override fun getAvailableDrivers(): List<AvailableDriver> {
        return availableDrivers.toList()
    }

    // Test helper
    fun addAvailableDriver(driver: AvailableDriver) {
        availableDrivers.add(driver)
    }

    fun clearDrivers() {
        availableDrivers.clear()
    }
}
