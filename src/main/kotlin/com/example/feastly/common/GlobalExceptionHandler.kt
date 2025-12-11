package com.example.feastly.common

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorResponse(
        val error: String,
        private val internalDetails: Map<String, String?>? = null
    ) {
        val details: Map<String, String?>?
            get() = internalDetails?.toMap()
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = "Validation failed", internalDetails = details))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        val msg = ex.rootCause?.message ?: ex.message ?: "Data integrity violation"
        val humanMessage = if (msg.contains("users") && msg.contains("email", ignoreCase = true)) {
            "Email already in use"
        } else {
            "Data integrity violation"
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = humanMessage))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(error = ex.message ?: "Resource not found"))

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(ex: MissingRequestHeaderException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = "Missing required header: ${ex.headerName}"))

    @ExceptionHandler(OrderAlreadyFinalizedException::class)
    fun handleOrderAlreadyFinalized(ex: OrderAlreadyFinalizedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: "Order cannot be modified"))

    @ExceptionHandler(UnauthorizedRestaurantAccessException::class)
    fun handleUnauthorizedRestaurant(ex: UnauthorizedRestaurantAccessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(error = ex.message ?: "Unauthorized access"))

    @ExceptionHandler(DriverAlreadyAssignedException::class)
    fun handleDriverAlreadyAssigned(ex: DriverAlreadyAssignedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: "Driver already assigned"))

    @ExceptionHandler(InvalidOrderStateForDispatchException::class)
    fun handleInvalidOrderStateForDispatch(ex: InvalidOrderStateForDispatchException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: "Invalid order state for dispatch"))

    @ExceptionHandler(UnauthorizedDriverAccessException::class)
    fun handleUnauthorizedDriver(ex: UnauthorizedDriverAccessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(error = ex.message ?: "Unauthorized driver access"))

    @ExceptionHandler(OrderAlreadyDeliveredException::class)
    fun handleOrderAlreadyDelivered(ex: OrderAlreadyDeliveredException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: "Order already delivered"))

    @ExceptionHandler(InvalidDeliveryStateException::class)
    fun handleInvalidDeliveryState(ex: InvalidDeliveryStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = ex.message ?: "Invalid delivery state"))
}
