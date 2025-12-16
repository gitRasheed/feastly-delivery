package com.feastly.dispatch.adapters

import com.feastly.events.AvailableDriver
import com.feastly.events.OrderInfo
import com.feastly.events.OrderStatus
import com.feastly.dispatch.ports.DriverStatusPort
import com.feastly.dispatch.ports.OrderQueryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.UUID

/**
 * REST client adapter for querying order information from order-service.
 * 
 * **IMPORTANT: Fallback only** - Use only for admin dashboards, retries, or 
 * operational tooling. Primary inter-service communication should use Kafka events.
 * 
 * Enable via: `dispatch.http-fallback.enabled=true`
 * 
 * TODO: Replace with OpenFeign for declarative HTTP clients
 */
@Component
@Primary
@ConditionalOnProperty(name = ["dispatch.http-fallback.enabled"], havingValue = "true")
class RestOrderQueryAdapter(
    private val restTemplate: RestTemplate,
    @Value("\${dispatch.order-service.base-url:http://order-service:8080}") 
    private val orderServiceBaseUrl: String
) : OrderQueryPort {

    private val logger = LoggerFactory.getLogger(RestOrderQueryAdapter::class.java)

    override fun getOrderInfo(orderId: UUID): OrderInfo? {
        return try {
            val url = "$orderServiceBaseUrl/api/internal/orders/$orderId/dispatch-info"
            logger.debug("Fetching order info from $url")
            restTemplate.getForObject(url, OrderInfo::class.java)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Order $orderId not found in order-service")
                null
            } else {
                logger.error("Failed to fetch order $orderId: ${e.message}")
                throw e
            }
        } catch (e: Exception) {
            logger.error("Error fetching order $orderId from order-service", e)
            throw e
        }
    }

    override fun updateOrderDriver(orderId: UUID, driverId: UUID?) {
        try {
            val url = "$orderServiceBaseUrl/api/orders/$orderId/assign-driver?driverId=$driverId"
            logger.debug("Updating order $orderId driver to $driverId")
            restTemplate.patchForObject(url, null, Void::class.java)
        } catch (e: Exception) {
            logger.error("Error updating driver for order $orderId", e)
            throw e
        }
    }

    override fun updateOrderStatus(orderId: UUID, status: OrderStatus) {
        try {
            val url = "$orderServiceBaseUrl/api/orders/$orderId/status"
            logger.debug("Updating order $orderId status to $status")
            restTemplate.patchForObject(url, mapOf("status" to status.name), Void::class.java)
        } catch (e: Exception) {
            logger.error("Error updating status for order $orderId", e)
            throw e
        }
    }
}

/**
 * DTO for driver status from driver-tracking-service REST API.
 * Maps to the service's DriverStatusResponse format.
 */
data class DriverStatusDto(
    val driverId: UUID,
    val isAvailable: Boolean,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Instant
) {
    fun toAvailableDriver() = AvailableDriver(
        driverId = driverId,
        latitude = latitude,
        longitude = longitude
    )
}

/**
 * REST client adapter for querying driver availability from driver-tracking-service.
 * 
 * **IMPORTANT: Fallback only** - Use only for admin dashboards, retries, or 
 * operational tooling. Primary inter-service communication should use Kafka events.
 * 
 * Enable via: `dispatch.http-fallback.enabled=true`
 */
@Component
@Primary
@ConditionalOnProperty(name = ["dispatch.http-fallback.enabled"], havingValue = "true")
class RestDriverStatusAdapter(
    private val restTemplate: RestTemplate,
    @Value("\${dispatch.driver-tracking-service.base-url:http://driver-tracking-service:8082}") 
    private val driverTrackingBaseUrl: String
) : DriverStatusPort {

    private val logger = LoggerFactory.getLogger(RestDriverStatusAdapter::class.java)

    override fun getAvailableDrivers(): List<AvailableDriver> {
        return try {
            val url = "$driverTrackingBaseUrl/api/drivers/available"
            logger.debug("Fetching available drivers from $url")
            val response = restTemplate.getForObject(url, Array<DriverStatusDto>::class.java)
            response?.map { it.toAvailableDriver() } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching available drivers", e)
            emptyList()
        }
    }
}
