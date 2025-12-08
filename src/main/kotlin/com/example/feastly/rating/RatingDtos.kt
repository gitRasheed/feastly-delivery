package com.example.feastly.rating

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class RatingRequest(
    @field:Min(1, message = "stars must be at least 1")
    @field:Max(5, message = "stars must be at most 5")
    val stars: Int,

    val comment: String? = null
)
