package com.feastly.dispatch

import com.feastly.dispatch.ports.DriverStatusPort
import com.feastly.dispatch.ports.OrderQueryPort
import com.feastly.events.DispatchAttemptStatus
import com.feastly.events.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

@Service
@Transactional
class DispatchService(
    private val orderQueryPort: OrderQueryPort,
    private val driverStatusPort: DriverStatusPort,
    private val dispatchAttemptRepository: DispatchAttemptRepository
) {
    private val logger = LoggerFactory.getLogger(DispatchService::class.java)

    companion object {
        private const val OFFER_TIMEOUT_SECONDS = 120L
    }

    /**
     * Start dispatch process for an accepted order.
     * Finds the nearest available driver who hasn't rejected this order.
     */
    fun startDispatch(orderId: UUID): DispatchAttempt? {
        val orderInfo = orderQueryPort.getOrderInfo(orderId)
            ?: run {
                logger.warn("Order $orderId not found")
                return null
            }

        // Preconditions
        if (orderInfo.status != OrderStatus.ACCEPTED) {
            logger.warn("Cannot dispatch order $orderId - not in ACCEPTED state: ${orderInfo.status}")
            return null
        }
        if (orderInfo.assignedDriverId != null) {
            logger.warn("Cannot dispatch order $orderId - already has driver: ${orderInfo.assignedDriverId}")
            return null
        }


        val availableDrivers = driverStatusPort.getAvailableDrivers()
        if (availableDrivers.isEmpty()) {
            logger.info("No available drivers for order $orderId")
            return null
        }


        val excludedDriverIds = dispatchAttemptRepository.findByOrderId(orderId)
            .filter { it.status in listOf(
                DispatchAttemptStatus.REJECTED,
                DispatchAttemptStatus.EXPIRED,
                DispatchAttemptStatus.CANCELLED
            ) }
            .map { it.driverId }
            .toSet()

        // Filter and sort by distance
        val eligibleDrivers = availableDrivers
            .filter { it.driverId !in excludedDriverIds }
            .sortedBy { driver ->
                calculateDistance(orderInfo.restaurantLat, orderInfo.restaurantLng, driver.latitude, driver.longitude)
            }

        if (eligibleDrivers.isEmpty()) {
            logger.info("No eligible drivers remaining for order $orderId")
            return null
        }

        // Offer to the first eligible driver
        val selectedDriver = eligibleDrivers.first()
        val attempt = DispatchAttempt(
            orderId = orderId,
            driverId = selectedDriver.driverId,
            status = DispatchAttemptStatus.PENDING
        )

        val saved = dispatchAttemptRepository.save(attempt)
        logger.info("Offered order $orderId to driver ${selectedDriver.driverId}")

        return saved
    }

    /**
     * Handle driver response to dispatch offer.
     */
    fun handleOfferResponse(orderId: UUID, driverId: UUID, accepted: Boolean): Boolean {
        val attempt = dispatchAttemptRepository.findByOrderIdAndDriverId(orderId, driverId)
            ?: run {
                logger.warn("No pending offer found for order $orderId and driver $driverId")
                return false
            }

        if (attempt.status != DispatchAttemptStatus.PENDING) {
            logger.warn("Offer for order $orderId already processed: ${attempt.status}")
            return false
        }

        attempt.respondedAt = Instant.now()

        if (accepted) {
            attempt.status = DispatchAttemptStatus.ACCEPTED
            orderQueryPort.updateOrderDriver(orderId, driverId)
            dispatchAttemptRepository.save(attempt)
            logger.info("Driver $driverId accepted order $orderId")
            return true
        } else {
            attempt.status = DispatchAttemptStatus.REJECTED
            dispatchAttemptRepository.save(attempt)
            logger.info("Driver $driverId rejected order $orderId - finding next driver")
            startDispatch(orderId)
            return true
        }
    }

    /**
     * Driver cancels their accepted assignment.
     */
    fun driverCancelAssignment(orderId: UUID, driverId: UUID): Boolean {
        val orderInfo = orderQueryPort.getOrderInfo(orderId)
            ?: run {
                logger.warn("Order $orderId not found")
                return false
            }

        if (orderInfo.assignedDriverId != driverId) {
            logger.warn("Driver $driverId is not assigned to order $orderId")
            return false
        }

        if (orderInfo.status !in listOf(OrderStatus.ACCEPTED, OrderStatus.DISPATCHED)) {
            logger.warn("Cannot cancel order $orderId - invalid status: ${orderInfo.status}")
            return false
        }

        // Update dispatch attempt
        val attempt = dispatchAttemptRepository.findByOrderIdAndDriverId(orderId, driverId)
        if (attempt != null && attempt.status == DispatchAttemptStatus.ACCEPTED) {
            attempt.status = DispatchAttemptStatus.CANCELLED
            attempt.respondedAt = Instant.now()
            dispatchAttemptRepository.save(attempt)
        }

        // Reset order
        orderQueryPort.updateOrderDriver(orderId, null)
        orderQueryPort.updateOrderStatus(orderId, OrderStatus.ACCEPTED)

        logger.info("Driver $driverId cancelled assignment for order $orderId")

        // Trigger re-dispatch
        startDispatch(orderId)

        return true
    }

    /**
     * Expire pending offers older than timeout.
     */
    fun expirePendingOffers(): Int {
        val cutoff = Instant.now().minusSeconds(OFFER_TIMEOUT_SECONDS)
        val staleOffers = dispatchAttemptRepository.findByStatusAndOfferedAtBefore(
            DispatchAttemptStatus.PENDING,
            cutoff
        )

        if (staleOffers.isEmpty()) {
            logger.info("No stale offers to expire")
            return 0
        }

        val orderIds = staleOffers.map { it.orderId }.toSet()

        staleOffers.forEach { offer ->
            offer.status = DispatchAttemptStatus.EXPIRED
            offer.respondedAt = Instant.now()
            dispatchAttemptRepository.save(offer)
            logger.info("Expired stale offer for order ${offer.orderId} to driver ${offer.driverId}")
        }

        // Re-dispatch for affected orders
        orderIds.forEach { orderId ->
            val orderInfo = orderQueryPort.getOrderInfo(orderId)
            if (orderInfo != null && orderInfo.status == OrderStatus.ACCEPTED && orderInfo.assignedDriverId == null) {
                startDispatch(orderId)
            }
        }

        logger.info("Expired ${staleOffers.size} stale offers across ${orderIds.size} orders")
        return staleOffers.size
    }

    /**
     * Get current dispatch status for an order.
     */
    @Transactional(readOnly = true)
    fun getDispatchStatus(orderId: UUID): DispatchStatusResponse {
        val orderInfo = orderQueryPort.getOrderInfo(orderId)
            ?: throw IllegalArgumentException("Order $orderId not found")

        val pendingOffer = dispatchAttemptRepository
            .findByOrderIdAndStatus(orderId, DispatchAttemptStatus.PENDING)
            .firstOrNull()

        return DispatchStatusResponse(
            orderId = orderId,
            currentDriverId = orderInfo.assignedDriverId,
            pendingOfferId = pendingOffer?.driverId,
            status = when {
                orderInfo.assignedDriverId != null -> "ASSIGNED"
                pendingOffer != null -> "PENDING_OFFER"
                else -> "AWAITING_DISPATCH"
            }
        )
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        return sqrt((lat2 - lat1).pow(2) + (lng2 - lng1).pow(2))
    }
}
