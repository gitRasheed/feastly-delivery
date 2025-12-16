package com.feastlydelivery.restaurant

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class RestaurantService(private val repository: RestaurantRepository) {

    fun create(request: CreateRestaurantRequest): Restaurant {
        val restaurant = Restaurant(name = request.name, description = request.description)
        return repository.save(restaurant)
    }

    fun findById(id: UUID): Restaurant =
        repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found: $id")

    fun findAll(): List<Restaurant> = repository.findAll()
}
