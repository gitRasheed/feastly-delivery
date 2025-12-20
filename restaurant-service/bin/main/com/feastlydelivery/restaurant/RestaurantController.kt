package com.feastlydelivery.restaurant

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalTime
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

    @PutMapping("/{id}/schedule")
    fun updateSchedule(
        @PathVariable id: UUID,
        @RequestBody request: RestaurantScheduleRequest
    ): ResponseEntity<Void> {
        service.updateSchedule(id, request)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/acceptance")
    fun updateAcceptance(
        @PathVariable id: UUID,
        @RequestBody request: RestaurantAcceptanceRequest
    ): ResponseEntity<Void> {
        service.updateAcceptance(id, request)
        return ResponseEntity.noContent().build()
    }
}

data class CreateRestaurantRequest(
    val ownerUserId: UUID,
    @field:NotBlank val name: String,
    val isOpen: Boolean? = false,
    val opensAt: LocalTime? = null,
    val closesAt: LocalTime? = null
)

data class RestaurantResponse(
    val id: UUID,
    val ownerUserId: UUID,
    val name: String,
    val isOpen: Boolean,
    val opensAt: LocalTime?,
    val closesAt: LocalTime?
)

fun Restaurant.toResponse() = RestaurantResponse(
    id = id,
    ownerUserId = ownerUserId,
    name = name,
    isOpen = isOpen,
    opensAt = opensAt,
    closesAt = closesAt
)

data class TimeWindow(
    val start: String,
    val end: String
)

data class RestaurantScheduleRequest(
    val weekly: Map<String, List<TimeWindow>>,
    val exceptions: Map<String, List<TimeWindow>>
)

data class RestaurantAcceptanceRequest(
    val isOnline: Boolean,
    val forcedOpenUntil: Instant?,
    val forcedClosedUntil: Instant?,
    val note: String? = null
)
