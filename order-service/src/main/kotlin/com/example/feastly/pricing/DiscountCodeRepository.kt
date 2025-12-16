package com.example.feastly.pricing

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DiscountCodeRepository : JpaRepository<DiscountCode, UUID> {
    fun findByCodeIgnoreCase(code: String): DiscountCode?
}
