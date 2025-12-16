package com.example.feastly.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Filter that injects traceId into MDC for all HTTP requests.
 * If X-Trace-Id header is present, it's used; otherwise a new UUID is generated.
 * 
 * TODO: Replace with OpenTelemetry auto-instrumentation for full distributed tracing
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TracingFilter : Filter {

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val traceId = httpRequest.getHeader(TRACE_ID_HEADER) ?: UUID.randomUUID().toString().take(8)

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }
}
