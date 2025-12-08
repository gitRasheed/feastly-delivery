package com.example.feastly.user

import com.example.feastly.order.OrderHistoryResponse
import com.example.feastly.order.OrderService
import com.example.feastly.order.toHistoryResponse
import com.example.feastly.rating.RatingRequest
import com.example.feastly.rating.RatingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val orderService: OrderService,
    private val ratingService: RatingService
) {
    @PostMapping
    fun register(@Valid @RequestBody dto: UserRegisterRequest): ResponseEntity<UserResponse> {
        val saved = userService.register(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping
    fun all(): List<UserResponse> = userService.list().map { it.toResponse() }

    @PostMapping("/{userId}/address")
    fun saveAddress(
        @PathVariable userId: UUID,
        @Valid @RequestBody dto: AddressRequest
    ): ResponseEntity<Void> {
        userService.saveAddress(userId, dto)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{userId}/orders")
    fun getOrderHistory(@PathVariable userId: UUID): List<OrderHistoryResponse> =
        orderService.getOrdersForUser(userId).map { it.toHistoryResponse() }

    @PostMapping("/{userId}/orders/{orderId}/rating")
    fun rateOrder(
        @PathVariable userId: UUID,
        @PathVariable orderId: UUID,
        @Valid @RequestBody dto: RatingRequest
    ): ResponseEntity<Void> {
        ratingService.rateOrder(userId, orderId, dto)
        return ResponseEntity.noContent().build()
    }
}
