package com.feastly.dispatch.config

import com.feastly.security.ServiceNameInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Configuration for REST clients used in fallback adapters.
 * Adds service identity header to all outbound requests.
 */
@Configuration
class RestClientConfig(
    @Value("\${spring.application.name}") private val serviceName: String
) {

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 10L
    }

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            .additionalInterceptors(ServiceNameInterceptor(serviceName))
            .build()
    }
}
