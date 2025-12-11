package com.example.feastly.dispatch

import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.driverstatus.DriverStatusService
import com.example.feastly.order.OrderRepository
import com.example.feastly.order.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

@Service
@Transactional
class DispatchService(
    private val orderRepository: OrderRepository,
    private val dispatchAttemptRepository: DispatchAttemptRepository,
    private val driverStatusService: DriverStatusService
) {
    private val logger = LoggerFactory.getLogger(DispatchService::class.java)

    /**
     * Start dispatch process for an accepted order.
     * Finds the nearest available driver who hasn't rejected this order.
     */
    fun startDispatch(orderId: UUID): DispatchAttempt? {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        // Preconditions
        if (order.status != OrderStatus.ACCEPTED) {
            logger.warn("Cannot dispatch order $orderId - not in ACCEPTED state: ${order.status}")
            return null
        }
        if (order.driverId != null) {
            logger.warn("Cannot dispatch order $orderId - already has driver: ${order.driverId}")
            return null
        }

        // Get restaurant location for distance calculation
        val restaurantLat = 40.7128 // Placeholder - would come from restaurant entity
        val restaurantLng = -74.0060

        // Get available drivers
        val availableDrivers = driverStatusService.getAvailableDrivers()
        if (availableDrivers.isEmpty()) {
            logger.info("No available drivers for order $orderId")
            return null
        }

        // Get drivers who already rejected/expired/cancelled for this order
        val excludedDriverIds = dispatchAttemptRepository.findByOrderId(orderId)
            .filter { it.status in listOf(
                DispatchAttemptStatus.REJECTED,
                DispatchAttemptStatus.EXPIRED,
                DispatchAttemptStatus.CANCELLED
            ) }
            .map { it.driverId }
            .toSet()

        // Filter and sort by distance (Euclidean placeholder)
        val eligibleDrivers = availableDrivers
            .filter { it.driverId !in excludedDriverIds }
            .sortedBy { driver ->
                calculateDistance(restaurantLat, restaurantLng, driver.latitude, driver.longitude)
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

        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        attempt.respondedAt = Instant.now()

        if (accepted) {
            attempt.status = DispatchAttemptStatus.ACCEPTED
            order.driverId = driverId
            order.updatedAt = Instant.now()
            orderRepository.save(order)
            logger.info("Driver $driverId accepted order $orderId")
            return true
        } else {
            attempt.status = DispatchAttemptStatus.REJECTED
            dispatchAttemptRepository.save(attempt)
            logger.info("Driver $driverId rejected order $orderId - finding next driver")
            // Recursively try next driver
            startDispatch(orderId)
            return true
        }
    }

    /**
     * Mark expired offers and retry dispatch.
     * Called by scheduler or manually.
     */
    fun expireStaleOffers(orderId: UUID) {
        val pendingOffers = dispatchAttemptRepository.findByOrderIdAndStatus(orderId, DispatchAttemptStatus.PENDING)
        val twoMinutesAgo = Instant.now().minusSeconds(120)

        pendingOffers
            .filter { it.offeredAt.isBefore(twoMinutesAgo) }
            .forEach { offer ->
                offer.status = DispatchAttemptStatus.EXPIRED
                offer.respondedAt = Instant.now()
                dispatchAttemptRepository.save(offer)
                logger.info("Offer to driver ${offer.driverId} for order $orderId expired")
            }

        // Retry dispatch if order still needs a driver
        val order = orderRepository.findByIdOrNull(orderId)
        if (order != null && order.status == OrderStatus.ACCEPTED && order.driverId == null) {
            startDispatch(orderId)
        }
    }

    /**
     * Get current dispatch status for an order.
     */
    @Transactional(readOnly = true)
    fun getDispatchStatus(orderId: UUID): DispatchStatusResponse {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        val pendingOffer = dispatchAttemptRepository
            .findByOrderIdAndStatus(orderId, DispatchAttemptStatus.PENDING)
            .firstOrNull()

        return DispatchStatusResponse(
            orderId = orderId,
            currentDriverId = order.driverId,
            pendingOfferId = pendingOffer?.driverId,
            status = if (order.driverId != null) "ASSIGNED" 
                     else if (pendingOffer != null) "PENDING_OFFER"
                     else "AWAITING_DISPATCH"
        )
    }

    /**
     * Driver cancels their accepted assignment.
     * Unassigns driver, resets order to ACCEPTED, and triggers re-dispatch.
     */
    fun driverCancelAssignment(orderId: UUID, driverId: UUID): Boolean {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        // Preconditions
        if (order.driverId != driverId) {
            logger.warn("Driver $driverId is not assigned to order $orderId")
            return false
        }

        if (order.status !in listOf(OrderStatus.ACCEPTED, OrderStatus.DISPATCHED)) {
            logger.warn("Cannot cancel order $orderId - invalid status: ${order.status}")
            return false
        }

        // Find and update the dispatch attempt
        val attempt = dispatchAttemptRepository.findByOrderIdAndDriverId(orderId, driverId)
        if (attempt != null && attempt.status == DispatchAttemptStatus.ACCEPTED) {
            attempt.status = DispatchAttemptStatus.CANCELLED
            attempt.respondedAt = Instant.now()
            dispatchAttemptRepository.save(attempt)
        }

        // Reset order
        order.driverId = null
        order.status = OrderStatus.ACCEPTED
        order.updatedAt = Instant.now()
        orderRepository.save(order)

        logger.info("Driver $driverId cancelled assignment for order $orderId")

        // Trigger re-dispatch
        startDispatch(orderId)

        return true
    }

    /**
     * Expire all pending offers older than 2 minutes globally.
     * Returns the count of expired offers.
     */
    fun expirePendingOffers(): Int {
        val cutoff = Instant.now().minusSeconds(120)
        val staleOffers = dispatchAttemptRepository.findByStatusAndOfferedAtBefore(
            DispatchAttemptStatus.PENDING,
            cutoff
        )

        if (staleOffers.isEmpty()) {
            logger.info("No stale offers to expire")
            return 0
        }

        // Group by orderId to avoid duplicate re-dispatch calls
        val orderIds = staleOffers.map { it.orderId }.toSet()

        staleOffers.forEach { offer ->
            offer.status = DispatchAttemptStatus.EXPIRED
            offer.respondedAt = Instant.now()
            dispatchAttemptRepository.save(offer)
            logger.info("Expired stale offer for order ${offer.orderId} to driver ${offer.driverId}")
        }

        // Re-dispatch for each affected order
        orderIds.forEach { orderId ->
            val order = orderRepository.findByIdOrNull(orderId)
            if (order != null && order.status == OrderStatus.ACCEPTED && order.driverId == null) {
                startDispatch(orderId)
            }
        }

        logger.info("Expired ${staleOffers.size} stale offers across ${orderIds.size} orders")
        return staleOffers.size
    }

    /**
     * Euclidean distance placeholder for driver matching.
     * In production, use Haversine formula for actual geo distance.
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        return sqrt((lat2 - lat1).pow(2) + (lng2 - lng1).pow(2))
    }
}
