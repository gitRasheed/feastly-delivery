package com.feastly.drivertracking

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(DriverStatusController::class)
class DriverStatusControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var driverStatusService: DriverStatusService

    @MockitoBean
    private lateinit var driverStatusRepository: DriverStatusRepository

    @MockitoBean
    private lateinit var driverLocationAggregator: DriverLocationAggregator

    @Test
    fun `GET driver-status returns 200 with list`() {
        val driverId = UUID.randomUUID()
        val driver = DriverStatus(
            driverId = driverId,
            isAvailable = true,
            latitude = 40.7128,
            longitude = -74.0060
        )

        whenever(driverStatusRepository.findAll()).thenReturn(listOf(driver))

        mockMvc.perform(get("/api/drivers/driver-status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].driverId").value(driverId.toString()))
            .andExpect(jsonPath("$[0].isAvailable").value(true))
    }

    @Test
    fun `GET driver-status returns empty list when no drivers`() {
        whenever(driverStatusRepository.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/drivers/driver-status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }
}
