package com.example.feastly.config

import com.example.feastly.client.RestaurantClient
import com.example.feastly.client.UserClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.UUID

@Configuration
@Profile("test")
class TestClientConfig {

    @Bean
    @Primary
    fun testUserClient(): UserClient = object : UserClient {
        override fun existsById(userId: UUID): Boolean = true
    }

    @Bean
    @Primary
    fun testRestaurantClient(): RestaurantClient = object : RestaurantClient {
        override fun existsById(restaurantId: UUID): Boolean = true
    }
}
