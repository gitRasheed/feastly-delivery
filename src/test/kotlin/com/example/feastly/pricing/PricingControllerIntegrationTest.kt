package com.example.feastly.pricing

import com.example.feastly.menu.MenuItemRequest
import com.example.feastly.menu.MenuItemResponse
import com.example.feastly.restaurant.RestaurantRegisterRequest
import com.example.feastly.restaurant.RestaurantResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PricingControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var discountCodeRepository: DiscountCodeRepository

    private fun url(path: String) = "http://localhost:$port$path"

    private fun createRestaurant(name: String): RestaurantResponse {
        val req = RestaurantRegisterRequest(name = name, address = "123 Test St", cuisine = "Test")
        return restTemplate.postForEntity(url("/api/restaurants"), req, RestaurantResponse::class.java).body!!
    }

    private fun createMenuItem(restaurantId: UUID, name: String, priceCents: Int): MenuItemResponse {
        val req = MenuItemRequest(name = name, priceCents = priceCents)
        return restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        ).body!!
    }

    @Test
    fun `preview pricing returns correct breakdown`() {
        val restaurant = createRestaurant("Pricing Test Restaurant ${UUID.randomUUID()}")
        val pizza = createMenuItem(restaurant.id, "Pizza", 1500)
        val drink = createMenuItem(restaurant.id, "Soda", 300)

        val request = PricingPreviewRequest(
            restaurantId = restaurant.id,
            items = listOf(
                PricingItemRequest(menuItemId = pizza.id, quantity = 2),
                PricingItemRequest(menuItemId = drink.id, quantity = 1)
            ),
            tipCents = 200
        )

        val response = restTemplate.postForEntity(
            url("/api/pricing/preview"),
            request,
            PricingPreviewResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val breakdown = response.body!!.breakdown

        // Subtotal: 2*1500 + 1*300 = 3300
        assertEquals(3300, breakdown.itemsSubtotalCents)
        // Service fee: 10% of 3300 = 330 (within min 99, max 999)
        assertEquals(330, breakdown.serviceFeeCents)
        // Delivery fee: flat 299
        assertEquals(299, breakdown.deliveryFeeCents)
        // No discount
        assertEquals(0, breakdown.discountCents)
        // Tip
        assertEquals(200, breakdown.tipCents)
        // Total: 3300 + 330 + 299 + 200 = 4129
        assertEquals(4129, breakdown.totalCents)
    }

    @Test
    fun `preview pricing with percent discount applied`() {
        val restaurant = createRestaurant("Discount Test Restaurant ${UUID.randomUUID()}")
        val burger = createMenuItem(restaurant.id, "Burger", 1200)

        // Create a 15% discount code
        val discountCode = DiscountCode(
            code = "SAVE15-${UUID.randomUUID()}",
            type = DiscountType.PERCENT,
            percentBps = 1500,
            scope = DiscountScope.ORDER_ITEMS_ONLY,
            isActive = true
        )
        discountCodeRepository.save(discountCode)

        val request = PricingPreviewRequest(
            restaurantId = restaurant.id,
            items = listOf(PricingItemRequest(menuItemId = burger.id, quantity = 2)),
            discountCode = discountCode.code,
            tipCents = 0
        )

        val response = restTemplate.postForEntity(
            url("/api/pricing/preview"),
            request,
            PricingPreviewResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val breakdown = response.body!!.breakdown

        // Subtotal: 2*1200 = 2400
        assertEquals(2400, breakdown.itemsSubtotalCents)
        // Discount: 15% of 2400 = 360
        assertEquals(360, breakdown.discountCents)
        // Total: 2400 + 240 (service) + 299 (delivery) - 360 (discount) = 2579
        assertEquals(2579, breakdown.totalCents)
    }

    @Test
    fun `preview pricing with unavailable item returns 409`() {
        val restaurant = createRestaurant("Unavailable Test Restaurant ${UUID.randomUUID()}")
        val soldOut = createMenuItem(restaurant.id, "Sold Out Item", 1000)

        // Mark item unavailable
        restTemplate.patchForObject(
            url("/api/restaurants/${restaurant.id}/menu/${soldOut.id}/availability?available=false"),
            null,
            Void::class.java
        )

        val request = PricingPreviewRequest(
            restaurantId = restaurant.id,
            items = listOf(PricingItemRequest(menuItemId = soldOut.id, quantity = 1))
        )

        val response = restTemplate.postForEntity(
            url("/api/pricing/preview"),
            request,
            String::class.java
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertTrue(response.body!!.contains("unavailable"))
    }
}
