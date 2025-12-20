package com.example.feastly.pricing

import java.util.UUID

data class PricingBreakdown(
    val itemsSubtotalCents: Int,
    val serviceFeeCents: Int,
    val deliveryFeeCents: Int,
    val discountCents: Int,
    val tipCents: Int,
    val totalCents: Int
)

data class PricingItemRequest(
    val menuItemId: UUID,
    val quantity: Int
)

data class PricingPreviewRequest(
    val restaurantId: UUID,
    val items: List<PricingItemRequest>,
    val discountCode: String? = null,
    val tipCents: Int? = 0
)

data class PricingPreviewResponse(
    val breakdown: PricingBreakdown
)
