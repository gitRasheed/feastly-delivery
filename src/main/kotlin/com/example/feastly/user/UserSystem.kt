package com.example.feastly.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserSystem(
    private val repo: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(request: UserRegisterRequest): User {
        val hashed = passwordEncoder.encode(request.password)
        val user = User(email = request.email, password = hashed)
        return repo.save(user)
    }
    fun list(): List<User> = repo.findAll()
}
