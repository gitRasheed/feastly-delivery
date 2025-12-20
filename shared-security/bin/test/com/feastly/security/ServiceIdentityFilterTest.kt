package com.feastly.security

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import jakarta.servlet.FilterChain
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.slf4j.MDC
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServiceIdentityFilterTest {

    private val filter = ServiceIdentityFilter()

    @Test
    fun `extracts service name from header and adds to MDC`() {
        val request = MockHttpServletRequest("GET", "/api/orders")
        request.addHeader("X-Service-Name", "dispatch-service")
        val response = MockHttpServletResponse()
        
        var capturedServiceName: String? = null
        val chain = FilterChain { _, _ ->
            capturedServiceName = MDC.get("callerService")
        }

        filter.doFilter(request, response, chain)

        assertEquals("dispatch-service", capturedServiceName)
        assertNull(MDC.get("callerService"))
    }

    @Test
    fun `uses UNKNOWN when service name header is missing`() {
        val request = MockHttpServletRequest("GET", "/api/orders")
        val response = MockHttpServletResponse()
        
        var capturedServiceName: String? = null
        val chain = FilterChain { _, _ ->
            capturedServiceName = MDC.get("callerService")
        }

        filter.doFilter(request, response, chain)

        assertEquals("UNKNOWN", capturedServiceName)
    }

    @Test
    fun `logs internal API access with caller identity`() {
        val request = MockHttpServletRequest("GET", "/api/internal/orders/123/dispatch-info")
        request.addHeader("X-Service-Name", "dispatch-service")
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }
}
