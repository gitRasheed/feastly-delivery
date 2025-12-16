package com.feastlydelivery.user

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
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
class UserController(private val service: UserService) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val user = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user.toResponse())
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): UserResponse = service.findById(id).toResponse()

    @GetMapping
    fun getAll(): List<UserResponse> = service.findAll().map { it.toResponse() }
}

data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:Email @field:NotBlank val email: String
)

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String
)

fun User.toResponse() = UserResponse(id = id, name = name, email = email)
