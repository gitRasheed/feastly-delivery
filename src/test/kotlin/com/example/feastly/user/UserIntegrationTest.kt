package com.example.feastly.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import java.util.*

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserIntegrationTest(
    @Autowired val userRepository: UserRepository
) {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @Container
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }
    }

    @Test
    fun `can save and retrieve a user`() {
        // Given
        val testUser = User(email = "test@example.com", password = "hashedpassword123")

        // When
        val savedUser = userRepository.save(testUser)
        val foundUser = userRepository.findById(savedUser.id).orElse(null)

        // Then
        assertNotNull(foundUser)
        assertEquals("test@example.com", foundUser?.email)
        assertEquals("hashedpassword123", foundUser?.password)
        assertNotNull(foundUser?.id)
    }

    @Test
    fun `can save multiple users with unique emails`() {
        // Given
        val user1 = User(email = "user1@example.com", password = "password1")
        val user2 = User(email = "user2@example.com", password = "password2")

        // When
        val savedUser1 = userRepository.save(user1)
        val savedUser2 = userRepository.save(user2)
        val allUsers = userRepository.findAll()

        // Then
        assertTrue(allUsers.size >= 2)
        assertTrue(allUsers.any { it.email == "user1@example.com" })
        assertTrue(allUsers.any { it.email == "user2@example.com" })
        assertNotEquals(savedUser1.id, savedUser2.id)
    }

    @Test
    fun `can delete a user`() {
        // Given
        val testUser = User(email = "delete@example.com", password = "password")
        val savedUser = userRepository.save(testUser)

        // When
        userRepository.deleteById(savedUser.id)
        val foundUser = userRepository.findById(savedUser.id).orElse(null)

        // Then
        assertNull(foundUser)
    }
}
