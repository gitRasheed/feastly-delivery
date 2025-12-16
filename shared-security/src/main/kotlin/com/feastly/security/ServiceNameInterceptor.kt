package com.feastly.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * Interceptor that adds X-Service-Name header to all outbound HTTP requests.
 * 
 * This identifies the calling service in service-to-service communication,
 * enabling logging and future access control.
 * 
 * Usage with RestTemplate:
 * ```
 * restTemplate.interceptors.add(ServiceNameInterceptor("dispatch-service"))
 * ```
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
