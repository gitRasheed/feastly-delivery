package com.feastlydelivery.restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class RestaurantAvailabilityService(
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(RestaurantAvailabilityService::class.java)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun computeAvailability(restaurant: Restaurant, at: Instant): RestaurantAvailabilityResponse {
        val result = computeAvailabilityInternal(restaurant, at)
        
        meterRegistry.counter(
            "restaurant_availability_checks_total",
            "result", if (result.accepting) "accepting" else "rejecting",
            "reason", result.reason.name
        ).increment()
        
        logger.info(
            "Availability check for restaurant {}: accepting={}, reason={}, nextChangeAt={}",
            restaurant.id, result.accepting, result.reason, result.nextChangeAt
        )
        
        return result
    }

    private fun computeAvailabilityInternal(restaurant: Restaurant, at: Instant): RestaurantAvailabilityResponse {
        // 1. Check forced open
        if (restaurant.forcedOpenUntil != null && restaurant.forcedOpenUntil!!.isAfter(at)) {
            return RestaurantAvailabilityResponse(
                accepting = true,
                reason = AvailabilityReason.FORCED_OPEN,
                nextChangeAt = restaurant.forcedOpenUntil
            )
        }

        // 2. Check forced closed
        if (restaurant.forcedClosedUntil != null && restaurant.forcedClosedUntil!!.isAfter(at)) {
            return RestaurantAvailabilityResponse(
                accepting = false,
                reason = AvailabilityReason.FORCED_CLOSED,
                nextChangeAt = restaurant.forcedClosedUntil
            )
        }

        // 3. Check if online
        if (!restaurant.isOnline) {
            return RestaurantAvailabilityResponse(
                accepting = false,
                reason = AvailabilityReason.OFFLINE,
                nextChangeAt = null
            )
        }

        // 4. Check schedule
        return checkSchedule(restaurant.scheduleJson, at)
    }

    private fun checkSchedule(scheduleJson: String, at: Instant): RestaurantAvailabilityResponse {
        val schedule = try {
            objectMapper.readValue<ScheduleData>(scheduleJson)
        } catch (e: Exception) {
            // Malformed JSON - treat as closed
            return RestaurantAvailabilityResponse(
                accepting = false,
                reason = AvailabilityReason.OUTSIDE_SCHEDULE,
                nextChangeAt = null
            )
        }

        val dateTime = at.atZone(ZoneOffset.UTC)
        val localDate = dateTime.toLocalDate()
        val localTime = dateTime.toLocalTime()
        val dateKey = localDate.toString() // Format: "2025-12-25"
        val dayOfWeek = localDate.dayOfWeek.name // "MONDAY", "TUESDAY", etc.

        // Check exception dates first
        val exceptionWindows = schedule.exceptions[dateKey]
        if (exceptionWindows != null) {
            // Exception exists for this date - use exception schedule (empty means closed)
            return checkTimeWindows(exceptionWindows, localTime, localDate, at)
        }

        // Check weekly schedule
        val weeklyWindows = schedule.weekly[dayOfWeek] ?: emptyList()
        return checkTimeWindows(weeklyWindows, localTime, localDate, at)
    }

    private fun checkTimeWindows(
        windows: List<TimeWindowData>,
        localTime: LocalTime,
        localDate: LocalDate,
        at: Instant
    ): RestaurantAvailabilityResponse {
        if (windows.isEmpty()) {
            return RestaurantAvailabilityResponse(
                accepting = false,
                reason = AvailabilityReason.OUTSIDE_SCHEDULE,
                nextChangeAt = null
            )
        }

        for (window in windows) {
            val start = LocalTime.parse(window.start, timeFormatter)
            val end = LocalTime.parse(window.end, timeFormatter)

            if (localTime >= start && localTime < end) {
                // Within this window - open
                val endInstant = localDate.atTime(end).toInstant(ZoneOffset.UTC)
                return RestaurantAvailabilityResponse(
                    accepting = true,
                    reason = AvailabilityReason.OPEN,
                    nextChangeAt = endInstant
                )
            }
        }

        // Outside all windows - find next opening
        val nextOpening = findNextOpening(windows, localTime, localDate)
        return RestaurantAvailabilityResponse(
            accepting = false,
            reason = AvailabilityReason.OUTSIDE_SCHEDULE,
            nextChangeAt = nextOpening
        )
    }

    private fun findNextOpening(
        windows: List<TimeWindowData>,
        localTime: LocalTime,
        localDate: LocalDate
    ): Instant? {
        // Find the next window start time that's after current time
        for (window in windows.sortedBy { LocalTime.parse(it.start, timeFormatter) }) {
            val start = LocalTime.parse(window.start, timeFormatter)
            if (start > localTime) {
                return localDate.atTime(start).toInstant(ZoneOffset.UTC)
            }
        }
        return null
    }

    // Internal data classes for JSON parsing
    private data class ScheduleData(
        val weekly: Map<String, List<TimeWindowData>> = emptyMap(),
        val exceptions: Map<String, List<TimeWindowData>> = emptyMap()
    )

    private data class TimeWindowData(
        val start: String,
        val end: String
    )
}
