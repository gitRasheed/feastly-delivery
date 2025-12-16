package com.feastly.security

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceNameInterceptorTest {

    @Test
    fun `adds X-Service-Name header to outbound requests`() {
        val interceptor = ServiceNameInterceptor("dispatch-service")
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("http://order-service:8080/api/orders"))
        val body = ByteArray(0)
        
        var headerValue: String? = null
        val execution = ClientHttpRequestExecution { req, _ ->
            headerValue = req.headers.getFirst("X-Service-Name")
            throw RuntimeException("Stop execution - header captured")
        }

        try {
            interceptor.intercept(request, body, execution)
        } catch (e: RuntimeException) {
            // Expected - we stopped execution after capturing header
        }

        assertEquals("dispatch-service", headerValue)
    }

    @Test
    fun `preserves existing headers`() {
        val interceptor = ServiceNameInterceptor("dispatch-service")
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("http://order-service:8080/api/orders"))
        request.headers.add("X-Trace-Id", "trace-123")
        val body = ByteArray(0)
        
        var traceId: String? = null
        var serviceName: String? = null
        val execution = ClientHttpRequestExecution { req, _ ->
            traceId = req.headers.getFirst("X-Trace-Id")
            serviceName = req.headers.getFirst("X-Service-Name")
            throw RuntimeException("Stop execution")
        }

        try {
            interceptor.intercept(request, body, execution)
        } catch (e: RuntimeException) {
            // Expected
        }

        assertEquals("trace-123", traceId)
        assertEquals("dispatch-service", serviceName)
    }
}
