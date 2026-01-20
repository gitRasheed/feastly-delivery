package com.feastly.dispatch.adapters

import com.feastly.dispatch.events.AvailableDriver
import com.feastly.dispatch.events.OrderInfo
import com.feastly.dispatch.events.OrderStatus
import com.feastly.dispatch.ports.DriverStatusPort
import com.feastly.dispatch.ports.OrderQueryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.UUID

/**
 * REST client for querying order info from order-service.
 * Fallback only - enable via `dispatch.http-fallback.enabled=true`.
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
        val url = "$orderServiceBaseUrl/api/internal/orders/$orderId/dispatch-info"
        logger.debug("Fetching order info from {}", url)
        return try {
            restTemplate.getForObject(url, OrderInfo::class.java)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) null else throw e
        }
    }

    override fun updateOrderDriver(orderId: UUID, driverId: UUID?) {
        val url = "$orderServiceBaseUrl/api/orders/$orderId/assign-driver?driverId=$driverId"
        restTemplate.patchForObject(url, null, Void::class.java)
    }

    override fun updateOrderStatus(orderId: UUID, status: OrderStatus) {
        val url = "$orderServiceBaseUrl/api/orders/$orderId/status"
        restTemplate.patchForObject(url, mapOf("status" to status.name), Void::class.java)
    }
}

data class DriverStatusDto(
    val driverId: UUID,
    val isAvailable: Boolean,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Instant
) {
    fun toAvailableDriver() = AvailableDriver(driverId, latitude, longitude)
}

/**
 * REST client for querying driver availability from driver-tracking-service.
 * Fallback only - enable via `dispatch.http-fallback.enabled=true`.
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
        val url = "$driverTrackingBaseUrl/api/drivers/available"
        logger.debug("Fetching available drivers from {}", url)
        return try {
            restTemplate.getForObject(url, Array<DriverStatusDto>::class.java)
                ?.map { it.toAvailableDriver() } ?: emptyList()
        } catch (e: HttpStatusCodeException) {
            logger.warn("Failed to fetch drivers: {}", e.statusCode)
            emptyList()
        }
    }
}
