package com.feastly.dispatch

import com.feastly.events.DispatchAttemptStatus
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

@WebMvcTest(DispatchController::class)
class DispatchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var dispatchService: DispatchService

    @MockitoBean
    private lateinit var dispatchAttemptRepository: DispatchAttemptRepository

    @Test
    fun `GET dispatch attempts returns 200 with list`() {
        val orderId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val attempt = DispatchAttempt(orderId = orderId, driverId = driverId)

        whenever(dispatchAttemptRepository.findAll()).thenReturn(listOf(attempt))

        mockMvc.perform(get("/api/dispatch/attempts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
            .andExpect(jsonPath("$[0].driverId").value(driverId.toString()))
            .andExpect(jsonPath("$[0].status").value(DispatchAttemptStatus.PENDING.name))
    }

    @Test
    fun `GET dispatch attempts returns empty list when no attempts`() {
        whenever(dispatchAttemptRepository.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/dispatch/attempts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }
}
