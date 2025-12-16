package com.example.feastly.menu

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class MenuItemRequest(
    @field:NotBlank(message = "name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "priceCents is required")
    @field:Min(value = 1, message = "priceCents must be at least 1")
    val priceCents: Int,

    val available: Boolean = true
)

data class MenuItemAvailabilityUpdateRequest(
    val isAvailable: Boolean
)

data class MenuItemResponse(
    val id: UUID,
    val restaurantId: UUID,
    val name: String,
    val description: String?,
    val priceCents: Int,
    val available: Boolean
)

fun MenuItem.toResponse() = MenuItemResponse(
    id = this.id,
    restaurantId = this.restaurantId,
    name = this.name,
    description = this.description,
    priceCents = this.priceCents,
    available = this.available
)

