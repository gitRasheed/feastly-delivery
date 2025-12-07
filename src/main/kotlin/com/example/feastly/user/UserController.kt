package com.example.feastly.user

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(private val system: UserService) {
    @PostMapping
    fun register(@Valid @RequestBody dto: UserRegisterRequest): ResponseEntity<UserResponse> {
        val saved = system.register(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping
    fun all(): List<UserResponse> = system.list().map { it.toResponse() }
}
