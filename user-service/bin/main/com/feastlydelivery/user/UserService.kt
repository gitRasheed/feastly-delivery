package com.feastlydelivery.user

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class UserService(private val repository: CustomerUserRepository) {

    fun create(request: CreateUserRequest): CustomerUser {
        val user = CustomerUser(
            email = request.email,
            passwordHash = request.password
        )
        return repository.save(user)
    }

    fun findById(id: UUID): CustomerUser =
        repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: $id")

    fun findAll(): List<CustomerUser> = repository.findAll()
}
