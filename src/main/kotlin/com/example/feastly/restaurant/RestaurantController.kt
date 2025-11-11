package com.example.feastly.restaurant

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/restaurants")
class RestaurantController(
    private val service: RestaurantService
) {
    @PostMapping
    fun register(@Valid @RequestBody request: RestaurantRegisterRequest): ResponseEntity<RestaurantResponse> {
        val saved = service.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping
    fun all(): List<RestaurantResponse> = service.list().map { it.toResponse() }
}

