package com.example.feastly.user

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val system: UserSystem) {
    @PostMapping
    fun register(@Valid @RequestBody dto: UserRegisterRequest): ResponseEntity<UserResponse> {
        val saved = system.register(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping
    fun all(): List<UserResponse> = system.list().map { it.toResponse() }
}
