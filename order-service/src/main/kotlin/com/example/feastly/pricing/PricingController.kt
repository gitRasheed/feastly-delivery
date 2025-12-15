package com.example.feastly.pricing

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pricing")
class PricingController(
    private val pricingService: PricingService
) {

    @PostMapping("/preview")
    fun previewPricing(
        @RequestBody @Valid request: PricingPreviewRequest
    ): PricingPreviewResponse {
        val breakdown = pricingService.previewPricing(request)
        return PricingPreviewResponse(breakdown = breakdown)
    }
}
