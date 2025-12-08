package com.example.feastly.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.*

// Request DTO for user registration with validation
data class UserRegisterRequest(
    @field:Email(message = "must be a valid email")
    @field:NotBlank(message = "email is required")
    val email: String,

    @field:NotBlank(message = "password is required")
    val password: String
)

// Response DTO to avoid exposing entity internals
data class UserResponse(
    val id: UUID,
    val email: String
)

// Request DTO for saving delivery address
data class AddressRequest(
    @field:NotBlank(message = "line1 is required")
    val line1: String,

    @field:NotBlank(message = "city is required")
    val city: String,

    @field:NotBlank(message = "postcode is required")
    val postcode: String
)

// Mapper
fun User.toResponse() = UserResponse(id = this.id, email = this.email)
