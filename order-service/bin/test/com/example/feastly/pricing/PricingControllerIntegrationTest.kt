package com.example.feastly.pricing

import com.example.feastly.BaseIntegrationTest
import com.example.feastly.TestRestaurantMenuClient
import com.example.feastly.client.MenuItemData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
class PricingControllerIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var discountCodeRepository: DiscountCodeRepository

    private fun url(path: String) = "http://localhost:$port$path"

    private fun testRestaurantId(): UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        TestRestaurantMenuClient.clearMenuItems()
    }

    private fun createMenuItem(restaurantId: UUID, name: String, priceCents: Int): MenuItemData {
        return TestRestaurantMenuClient.registerMenuItem(
            restaurantId = restaurantId,
            name = name,
            priceCents = priceCents
        )
    }

    @Test
    fun `preview pricing returns correct breakdown`() {
        val restaurantId = testRestaurantId()
        val pizza = createMenuItem(restaurantId, "Pizza", 1500)
        val drink = createMenuItem(restaurantId, "Soda", 300)

        val request = PricingPreviewRequest(
            restaurantId = restaurantId,

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
        // Tip: 200
        assertEquals(200, breakdown.tipCents)
        // Total: 3300 + 330 + 299 + 200 = 4129
        assertEquals(4129, breakdown.totalCents)
    }

    @Test
    fun `preview pricing with discount code applies discount`() {
        val restaurantId = testRestaurantId()
        val pizza = createMenuItem(restaurantId, "Pizza", 2000)

        discountCodeRepository.save(
            DiscountCode(
                code = "SAVE10",
                percentage = 10,
                active = true
            )
        )

        val request = PricingPreviewRequest(
            restaurantId = restaurantId,
            items = listOf(PricingItemRequest(menuItemId = pizza.id, quantity = 2)),
            discountCode = "SAVE10"
        )

        val response = restTemplate.postForEntity(
            url("/api/pricing/preview"),
            request,
            PricingPreviewResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val breakdown = response.body!!.breakdown

        // Subtotal: 2*2000 = 4000
        assertEquals(4000, breakdown.itemsSubtotalCents)
        // Discount: 10% of 4000 = 400
        assertEquals(400, breakdown.discountCents)
    }

    @Test
    fun `preview pricing with unavailable item returns 409`() {
        val restaurantId = testRestaurantId()
        val soldOut = createMenuItem(restaurantId, "Sold Out Item", 1000)

        TestRestaurantMenuClient.setAvailability(soldOut.id, false)

        val request = PricingPreviewRequest(
            restaurantId = restaurantId,
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
