package com.feastlydelivery.user

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class UserService(private val repository: UserRepository) {

    fun create(request: CreateUserRequest): User {
        val user = User(name = request.name, email = request.email)
        return repository.save(user)
    }

    fun findById(id: UUID): User =
        repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: $id")

    fun findAll(): List<User> = repository.findAll()
}
