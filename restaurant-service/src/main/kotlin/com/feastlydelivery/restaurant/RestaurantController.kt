package com.feastlydelivery.restaurant

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/restaurants")
class RestaurantController(private val service: RestaurantService) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateRestaurantRequest): ResponseEntity<RestaurantResponse> {
        val restaurant = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurant.toResponse())
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): RestaurantResponse = service.findById(id).toResponse()

    @GetMapping
    fun getAll(): List<RestaurantResponse> = service.findAll().map { it.toResponse() }
}

data class CreateRestaurantRequest(
    @field:NotBlank val name: String,
    val description: String? = null
)

data class RestaurantResponse(
    val id: UUID,
    val name: String,
    val description: String?
)

fun Restaurant.toResponse() = RestaurantResponse(id = id, name = name, description = description)
