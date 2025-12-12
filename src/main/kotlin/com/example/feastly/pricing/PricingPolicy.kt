package com.example.feastly.pricing

data class PricingPolicy(
    val serviceFeeBps: Int,
    val deliveryFeeFlatCents: Int,
    val minServiceFeeCents: Int = 0,
    val maxServiceFeeCents: Int? = null
)
