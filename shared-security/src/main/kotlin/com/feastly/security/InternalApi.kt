package com.feastly.security

/**
 * Marks an endpoint as internal-only, intended for service-to-service communication.
 * 
 * Endpoints annotated with @InternalApi:
 * - Should only be accessed by other services, not external clients
 * - Are logged with caller identity when accessed
 * - Will be secured with service-to-service auth in future phases
 * 
 * Example usage:
 * ```
 * @InternalApi
 * @GetMapping("/api/internal/orders/{id}/dispatch-info")
 * fun getDispatchInfo(@PathVariable id: UUID): OrderInfo
 * ```
 * 
 * TODO: Enforce access control based on service identity
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InternalApi(
    /**
     * Description of why this endpoint is internal.
     */
    val reason: String = "Service-to-service communication only"
)
