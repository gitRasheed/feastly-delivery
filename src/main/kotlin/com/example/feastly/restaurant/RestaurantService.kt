package com.example.feastly.restaurant

import org.springframework.stereotype.Service

@Service
class RestaurantService(
    private val repository: RestaurantRepository
) {
    fun register(request: RestaurantRegisterRequest): Restaurant {
        val restaurant = Restaurant(
            name = request.name,
            address = request.address,
            cuisine = request.cuisine
        )
        return repository.save(restaurant)
    }

    fun list(): List<Restaurant> = repository.findAll()
}

