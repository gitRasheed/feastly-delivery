package com.example.feastly.restaurant

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class RestaurantRegisterRequest(
    @field:NotBlank(message = "name is required") val name: String,
    @field:NotBlank(message = "address is required") val address: String,
    @field:NotBlank(message = "cuisine is required") val cuisine: String,
)

data class RestaurantResponse(
    val id: UUID,
    val name: String,
    val address: String,
    val cuisine: String,
)

fun Restaurant.toResponse() = RestaurantResponse(
    id = this.id,
    name = this.name,
    address = this.address,
    cuisine = this.cuisine,
)

