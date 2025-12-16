package com.feastly.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Extracts X-Service-Name header and adds to MDC for logging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class ServiceIdentityFilter : Filter {

    private val logger = LoggerFactory.getLogger(ServiceIdentityFilter::class.java)

    companion object {
        const val SERVICE_NAME_HEADER = "X-Service-Name"
        const val SERVICE_NAME_MDC_KEY = "callerService"
        const val UNKNOWN_SERVICE = "UNKNOWN"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val callerService = httpRequest.getHeader(SERVICE_NAME_HEADER) ?: UNKNOWN_SERVICE

        try {
            MDC.put(SERVICE_NAME_MDC_KEY, callerService)
            
            if (httpRequest.requestURI.contains("/api/internal/")) {
                logger.info("Internal API access: {} {} (caller: {})", 
                    httpRequest.method, 
                    httpRequest.requestURI, 
                    callerService
                )
            }
            
            chain.doFilter(request, response)
        } finally {
            MDC.remove(SERVICE_NAME_MDC_KEY)
        }
    }
}
