package com.feastly.dispatch.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.feastly.events.AvailableDriver
import com.feastly.events.OrderInfo
import com.feastly.events.OrderStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for REST client adapters used as fallback for cross-service communication.
 */
class RestClientAdaptersTest {

    private lateinit var restTemplate: RestTemplate
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var objectMapper: ObjectMapper

    private val orderServiceBaseUrl = "http://order-service:8080"
    private val driverTrackingBaseUrl = "http://driver-tracking-service:8082"

    @BeforeEach
    fun setup() {
        restTemplate = RestTemplate()
        mockServer = MockRestServiceServer.createServer(restTemplate)
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    fun `getOrderInfo returns order info on success`() {
        val orderId = UUID.randomUUID()
        val expectedOrderInfo = OrderInfo(
            orderId = orderId,
            status = OrderStatus.ACCEPTED,
            assignedDriverId = null,
            restaurantLat = 40.7128,
            restaurantLng = -74.0060
        )

        mockServer.expect(requestTo("$orderServiceBaseUrl/api/internal/orders/$orderId/dispatch-info"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(expectedOrderInfo), MediaType.APPLICATION_JSON))

        val adapter = RestOrderQueryAdapter(restTemplate, orderServiceBaseUrl)
        val result = adapter.getOrderInfo(orderId)

        mockServer.verify()
        assertNotNull(result)
        assertEquals(orderId, result.orderId)
        assertEquals(OrderStatus.ACCEPTED, result.status)
    }

    @Test
    fun `getOrderInfo returns null on 404`() {
        val orderId = UUID.randomUUID()

        mockServer.expect(requestTo("$orderServiceBaseUrl/api/internal/orders/$orderId/dispatch-info"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        val adapter = RestOrderQueryAdapter(restTemplate, orderServiceBaseUrl)
        val result = adapter.getOrderInfo(orderId)

        mockServer.verify()
        assertNull(result)
    }

    @Test
    fun `getAvailableDrivers returns list of drivers on success`() {
        val driver1 = DriverStatusDto(
            driverId = UUID.randomUUID(),
            isAvailable = true,
            latitude = 40.7128,
            longitude = -74.0060,
            lastUpdated = Instant.now()
        )
        val driver2 = DriverStatusDto(
            driverId = UUID.randomUUID(),
            isAvailable = true,
            latitude = 40.7580,
            longitude = -73.9855,
            lastUpdated = Instant.now()
        )

        mockServer.expect(requestTo("$driverTrackingBaseUrl/api/drivers/available"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(listOf(driver1, driver2)), MediaType.APPLICATION_JSON))

        val adapter = RestDriverStatusAdapter(restTemplate, driverTrackingBaseUrl)
        val result = adapter.getAvailableDrivers()

        mockServer.verify()
        assertEquals(2, result.size)
        assertEquals(driver1.driverId, result[0].driverId)
        assertEquals(driver2.driverId, result[1].driverId)
    }

    @Test
    fun `getAvailableDrivers returns empty list on error`() {
        mockServer.expect(requestTo("$driverTrackingBaseUrl/api/drivers/available"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        val adapter = RestDriverStatusAdapter(restTemplate, driverTrackingBaseUrl)
        val result = adapter.getAvailableDrivers()

        mockServer.verify()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAvailableDrivers returns empty list on empty response`() {
        mockServer.expect(requestTo("$driverTrackingBaseUrl/api/drivers/available"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        val adapter = RestDriverStatusAdapter(restTemplate, driverTrackingBaseUrl)
        val result = adapter.getAvailableDrivers()

        mockServer.verify()
        assertTrue(result.isEmpty())
    }
}
