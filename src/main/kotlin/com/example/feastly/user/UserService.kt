package com.example.feastly.user

import com.example.feastly.common.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val repo: UserRepository,
    private val addressRepository: AddressRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun register(request: UserRegisterRequest): User {
        val hashed = passwordEncoder.encode(request.password)
        val user = User(email = request.email, password = hashed)
        return repo.save(user)
    }

    fun list(): List<User> = repo.findAll()

    @Transactional
    fun saveAddress(userId: UUID, request: AddressRequest): Address {
        val user = repo.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val address = Address(
            user = user,
            line1 = request.line1,
            city = request.city,
            postcode = request.postcode
        )
        return addressRepository.save(address)
    }
}
