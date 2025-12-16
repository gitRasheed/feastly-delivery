package com.feastly.security

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * Adds X-Service-Name header to outbound HTTP requests for service identity tracking.
 */
class ServiceNameInterceptor(
    private val serviceName: String
) : ClientHttpRequestInterceptor {

    companion object {
        const val SERVICE_NAME_HEADER = "X-Service-Name"
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        request.headers.add(SERVICE_NAME_HEADER, serviceName)
        return execution.execute(request, body)
    }
}
