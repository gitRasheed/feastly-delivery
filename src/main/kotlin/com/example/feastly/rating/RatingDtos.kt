package com.example.feastly.rating

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.UUID

private const val MIN_STARS = 1
private const val MAX_STARS = 5

data class RatingRequest(
    @field:Min(MIN_STARS.toLong(), message = "stars must be at least $MIN_STARS")
    @field:Max(MAX_STARS.toLong(), message = "stars must be at most $MAX_STARS")
    val stars: Int,

    val comment: String? = null
)

data class RatingResponse(
    val id: UUID,
    val orderId: UUID,
    val stars: Int,
    val comment: String?,
    val createdAt: Instant
)

