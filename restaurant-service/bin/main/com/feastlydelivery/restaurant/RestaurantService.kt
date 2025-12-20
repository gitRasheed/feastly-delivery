package com.feastlydelivery.restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class RestaurantService(
    private val repository: RestaurantRepository,
    private val objectMapper: ObjectMapper
) {

    fun create(request: CreateRestaurantRequest): Restaurant {
        val restaurant = Restaurant(
            ownerUserId = request.ownerUserId,
            name = request.name,
            isOpen = request.isOpen ?: false,
            opensAt = request.opensAt,
            closesAt = request.closesAt
        )
        return repository.save(restaurant)
    }

    fun findById(id: UUID): Restaurant =
        repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found: $id")

    fun findAll(): List<Restaurant> = repository.findAll()

    @Transactional
    fun updateSchedule(id: UUID, request: RestaurantScheduleRequest) {
        val restaurant = findById(id)
        val scheduleMap = mapOf("weekly" to request.weekly, "exceptions" to request.exceptions)
        restaurant.scheduleJson = objectMapper.writeValueAsString(scheduleMap)
        repository.save(restaurant)
    }

    @Transactional
    fun updateAcceptance(id: UUID, request: RestaurantAcceptanceRequest) {
        val restaurant = findById(id)

        val now = Instant.now()
        if (request.forcedOpenUntil != null && request.forcedClosedUntil != null &&
            request.forcedOpenUntil.isAfter(now) && request.forcedClosedUntil.isAfter(now)
        ) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot set both forcedOpenUntil and forcedClosedUntil in the future"
            )
        }

        restaurant.isOnline = request.isOnline
        restaurant.forcedOpenUntil = request.forcedOpenUntil
        restaurant.forcedClosedUntil = request.forcedClosedUntil
        repository.save(restaurant)
    }
}
