package com.example.feastly.pricing

import java.util.UUID

interface PricingService {
    fun previewPricing(request: PricingPreviewRequest): PricingBreakdown
    fun priceExistingOrder(orderId: UUID, discountCode: String?, tipCents: Int): PricingBreakdown
}
