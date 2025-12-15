package com.example.feastly.user

import com.example.feastly.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class UserIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Test
    fun `can save and retrieve a user`() {
        val testUser = User(email = "test@example.com", password = "hashedpassword123")

        val savedUser = userRepository.save(testUser)
        val foundUser = userRepository.findById(savedUser.id).orElse(null)

        assertNotNull(foundUser)
        assertEquals("test@example.com", foundUser?.email)
        assertEquals("hashedpassword123", foundUser?.password)
        assertNotNull(foundUser?.id)
    }

    @Test
    fun `can save multiple users with unique emails`() {
        val user1 = User(email = "user1@example.com", password = "password1")
        val user2 = User(email = "user2@example.com", password = "password2")

        val savedUser1 = userRepository.save(user1)
        val savedUser2 = userRepository.save(user2)
        val allUsers = userRepository.findAll()

        assertTrue(allUsers.size >= 2)
        assertTrue(allUsers.any { it.email == "user1@example.com" })
        assertTrue(allUsers.any { it.email == "user2@example.com" })
        assertNotEquals(savedUser1.id, savedUser2.id)
    }

    @Test
    fun `can delete a user`() {
        val testUser = User(email = "delete@example.com", password = "password")
        val savedUser = userRepository.save(testUser)

        userRepository.deleteById(savedUser.id)
        val foundUser = userRepository.findById(savedUser.id).orElse(null)

        assertNull(foundUser)
    }
}
