package com.example.feastly.pricing

import com.example.feastly.common.MenuItemNotFoundException
import com.example.feastly.common.MenuItemUnavailableException
import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.menu.MenuItemRepository
import com.example.feastly.order.OrderRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@Service
class DefaultPricingService(
    private val menuItemRepository: MenuItemRepository,
    private val orderRepository: OrderRepository,
    private val discountCodeRepository: DiscountCodeRepository,
    private val pricingPolicy: PricingPolicy
) : PricingService {

    companion object {
        private const val BPS_DIVISOR = 10_000
    }

    override fun previewPricing(request: PricingPreviewRequest): PricingBreakdown {
        val menuItems = request.items.map { itemRequest ->
            val menuItem = menuItemRepository.findByIdOrNull(itemRequest.menuItemId)
                ?: throw MenuItemNotFoundException(itemRequest.menuItemId)

            require(menuItem.restaurant.id == request.restaurantId) {
                "Menu item ${menuItem.id} does not belong to restaurant ${request.restaurantId}"
            }

            if (!menuItem.available) {
                throw MenuItemUnavailableException(menuItem.id)
            }

            menuItem to itemRequest.quantity
        }

        val itemsSubtotalCents = menuItems.sumOf { (item, qty) -> item.priceCents * qty }
        val serviceFeeCents = computeServiceFee(itemsSubtotalCents)
        val deliveryFeeCents = pricingPolicy.deliveryFeeFlatCents
        val discountCents = computeDiscount(request.discountCode, itemsSubtotalCents)

        val rawTip = request.tipCents ?: 0
        require(rawTip >= 0) { "Tip cannot be negative" }
        val tipCents = minOf(rawTip, itemsSubtotalCents)

        val totalCents = itemsSubtotalCents + serviceFeeCents + deliveryFeeCents - discountCents + tipCents

        return PricingBreakdown(
            itemsSubtotalCents = itemsSubtotalCents,
            serviceFeeCents = serviceFeeCents,
            deliveryFeeCents = deliveryFeeCents,
            discountCents = discountCents,
            tipCents = tipCents,
            totalCents = totalCents
        )
    }

    override fun priceExistingOrder(
        orderId: UUID,
        discountCode: String?,
        tipCents: Int
    ): PricingBreakdown {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        val itemsSubtotalCents = order.items.sumOf { it.priceCents * it.quantity }
        val serviceFeeCents = computeServiceFee(itemsSubtotalCents)
        val deliveryFeeCents = pricingPolicy.deliveryFeeFlatCents
        val discountCents = computeDiscount(discountCode, itemsSubtotalCents)

        require(tipCents >= 0) { "Tip cannot be negative" }
        val cappedTipCents = minOf(tipCents, itemsSubtotalCents)

        val totalCents = itemsSubtotalCents + serviceFeeCents + deliveryFeeCents - discountCents + cappedTipCents

        return PricingBreakdown(
            itemsSubtotalCents = itemsSubtotalCents,
            serviceFeeCents = serviceFeeCents,
            deliveryFeeCents = deliveryFeeCents,
            discountCents = discountCents,
            tipCents = cappedTipCents,
            totalCents = totalCents
        )
    }

    private fun computeServiceFee(subtotalCents: Int): Int {
        val rawFee = (subtotalCents * pricingPolicy.serviceFeeBps) / BPS_DIVISOR
        val withMin = max(rawFee, pricingPolicy.minServiceFeeCents)
        return pricingPolicy.maxServiceFeeCents?.let { min(withMin, it) } ?: withMin
    }

    private fun computeDiscount(code: String?, itemsSubtotalCents: Int): Int {
        if (code.isNullOrBlank()) return 0

        val discount = discountCodeRepository.findByCodeIgnoreCase(code) ?: return 0
        if (!discount.isActive) return 0

        discount.minItemsSubtotalCents?.let { minRequired ->
            if (itemsSubtotalCents < minRequired) return 0
        }

        return when (discount.type) {
            DiscountType.PERCENT -> {
                val bps = discount.percentBps ?: 0
                (itemsSubtotalCents * bps) / BPS_DIVISOR
            }
            DiscountType.FIXED_CENTS -> {
                val fixed = discount.fixedCents ?: 0
                min(fixed, itemsSubtotalCents)
            }
        }
    }
}
