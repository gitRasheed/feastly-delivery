package com.feastly.security

/**
 * Marks an endpoint as internal-only for service-to-service communication.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InternalApi(
    val reason: String = "Service-to-service communication only"
)
