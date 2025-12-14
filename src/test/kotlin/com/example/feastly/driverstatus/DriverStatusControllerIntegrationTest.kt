package com.example.feastly.driverstatus

import com.example.feastly.BaseIntegrationTest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DriverStatusControllerIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    @Test
    fun `driver can update their availability status`() {
        val driverId = UUID.randomUUID()
        val request = DriverStatusUpdateRequest(
            isAvailable = true,
            latitude = 40.7128,
            longitude = -74.0060
        )

        val response = restTemplate.exchange(
            url("/api/drivers/$driverId/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(request),
            DriverStatusResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(driverId, response.body!!.driverId)
        assertTrue(response.body!!.isAvailable)
        assertEquals(40.7128, response.body!!.latitude)
        assertEquals(-74.0060, response.body!!.longitude)
    }

    @Test
    fun `driver can update location and toggle availability off`() {
        val driverId = UUID.randomUUID()

        // First set available
        val availableRequest = DriverStatusUpdateRequest(
            isAvailable = true,
            latitude = 34.0522,
            longitude = -118.2437
        )
        restTemplate.exchange(
            url("/api/drivers/$driverId/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(availableRequest),
            DriverStatusResponse::class.java
        )

        // Then set unavailable with new location
        val unavailableRequest = DriverStatusUpdateRequest(
            isAvailable = false,
            latitude = 34.0530,
            longitude = -118.2450
        )
        val response = restTemplate.exchange(
            url("/api/drivers/$driverId/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(unavailableRequest),
            DriverStatusResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertFalse(response.body!!.isAvailable)
        assertEquals(34.0530, response.body!!.latitude)
    }

    @Test
    fun `can retrieve driver status by id`() {
        val driverId = UUID.randomUUID()
        val request = DriverStatusUpdateRequest(
            isAvailable = true,
            latitude = 51.5074,
            longitude = -0.1278
        )

        restTemplate.exchange(
            url("/api/drivers/$driverId/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(request),
            DriverStatusResponse::class.java
        )

        val getResponse = restTemplate.getForEntity(
            url("/api/drivers/$driverId/status"),
            DriverStatusResponse::class.java
        )

        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals(driverId, getResponse.body!!.driverId)
    }

    @Test
    fun `get status for non-existent driver returns 404`() {
        val fakeDriverId = UUID.randomUUID()

        val response = restTemplate.getForEntity(
            url("/api/drivers/$fakeDriverId/status"),
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `can list available drivers`() {
        val driver1 = UUID.randomUUID()
        val driver2 = UUID.randomUUID()
        val driver3 = UUID.randomUUID()

        // Set driver1 and driver2 as available
        restTemplate.exchange(
            url("/api/drivers/$driver1/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(DriverStatusUpdateRequest(true, 40.0, -74.0)),
            DriverStatusResponse::class.java
        )
        restTemplate.exchange(
            url("/api/drivers/$driver2/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(DriverStatusUpdateRequest(true, 41.0, -73.0)),
            DriverStatusResponse::class.java
        )
        // Set driver3 as unavailable
        restTemplate.exchange(
            url("/api/drivers/$driver3/status"),
            org.springframework.http.HttpMethod.PUT,
            org.springframework.http.HttpEntity(DriverStatusUpdateRequest(false, 42.0, -72.0)),
            DriverStatusResponse::class.java
        )

        val response = restTemplate.getForEntity(
            url("/api/drivers/available"),
            Array<DriverStatusResponse>::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val availableDrivers = response.body!!
        assertTrue(availableDrivers.any { it.driverId == driver1 })
        assertTrue(availableDrivers.any { it.driverId == driver2 })
        assertFalse(availableDrivers.any { it.driverId == driver3 })
    }
}
