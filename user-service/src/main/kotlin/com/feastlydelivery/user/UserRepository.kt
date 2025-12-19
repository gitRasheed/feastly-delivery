package com.feastlydelivery.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomerUserRepository : JpaRepository<CustomerUser, UUID> {
    fun findByEmail(email: String): CustomerUser?
}

interface DriverUserRepository : JpaRepository<DriverUser, UUID> {
    fun findByEmail(email: String): DriverUser?
}

interface RestaurantUserRepository : JpaRepository<RestaurantUser, UUID> {
    fun findByEmail(email: String): RestaurantUser?
}
