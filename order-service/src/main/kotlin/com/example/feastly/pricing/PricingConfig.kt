package com.example.feastly.pricing

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PricingConfig {

    @Bean
    fun pricingPolicy(): PricingPolicy = PricingPolicy(
        serviceFeeBps = 1000,
        deliveryFeeFlatCents = 299,
        minServiceFeeCents = 99,
        maxServiceFeeCents = 999
    )
}
