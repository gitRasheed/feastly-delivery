package com.example.feastly.restaurant

import com.feastly.events.RestaurantCreatedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class RestaurantService(
    private val repository: RestaurantRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    companion object {
        private const val RESTAURANT_EVENTS_TOPIC = "restaurant.events"
    }

    fun register(request: RestaurantRegisterRequest): Restaurant {
        val restaurant = Restaurant(
            name = request.name,
            address = request.address,
            cuisine = request.cuisine
        )
        val saved = repository.save(restaurant)

        val event = RestaurantCreatedEvent(
            restaurantId = saved.id,
            name = saved.name
        )
        kafkaTemplate.send(RESTAURANT_EVENTS_TOPIC, saved.id.toString(), event)

        return saved
    }

    fun list(): List<Restaurant> = repository.findAll()
}
