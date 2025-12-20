package com.feastlydelivery.restaurant

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/internal/restaurants")
class RestaurantAvailabilityController(
    private val restaurantService: RestaurantService,
    private val availabilityService: RestaurantAvailabilityService
) {

    @GetMapping("/{id}/availability")
    fun getAvailability(
        @PathVariable id: UUID,
        @RequestParam at: Instant?
    ): RestaurantAvailabilityResponse {
        val restaurant = restaurantService.findById(id)
        val queryTime = at ?: Instant.now()
        return availabilityService.computeAvailability(restaurant, queryTime)
    }
}
