package com.example.feastly.restaurant

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RestaurantRepository : JpaRepository<Restaurant, UUID>

