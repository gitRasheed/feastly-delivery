package com.example.feastly.menu

import com.example.feastly.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MenuItemControllerIntegrationTest : BaseIntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private lateinit var restaurantId: UUID

    private fun url(path: String) = "http://localhost:$port$path"

    @BeforeEach
    fun setup() {
        restaurantId = UUID.randomUUID()
    }

    @Test
    fun `add menu item returns 201`() {
        val req = MenuItemRequest(
            name = "Margherita Pizza",
            description = "Fresh tomatoes, mozzarella, basil",
            priceCents = 1299
        )

        val response = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Margherita Pizza", response.body!!.name)
        assertEquals(1299, response.body!!.priceCents)
        assertTrue(response.body!!.available)
    }

    @Test
    fun `get menu returns items for restaurant`() {
        val req1 = MenuItemRequest(name = "Pizza", priceCents = 1200)
        val req2 = MenuItemRequest(name = "Pasta", priceCents = 1100)

        restTemplate.postForEntity(url("/api/restaurants/$restaurantId/menu"), req1, MenuItemResponse::class.java)
        restTemplate.postForEntity(url("/api/restaurants/$restaurantId/menu"), req2, MenuItemResponse::class.java)

        val response = restTemplate.getForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            Array<MenuItemResponse>::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.size >= 2)
        assertTrue(response.body!!.any { it.name == "Pizza" })
        assertTrue(response.body!!.any { it.name == "Pasta" })
    }

    @Test
    fun `update menu item returns 200`() {
        val createReq = MenuItemRequest(name = "Old Name", priceCents = 1000)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            createReq,
            MenuItemResponse::class.java
        ).body!!

        val updateReq = MenuItemRequest(name = "New Name", priceCents = 1500)
        val response = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu/${created.id}"),
            HttpMethod.PUT,
            HttpEntity(updateReq),
            MenuItemResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("New Name", response.body!!.name)
        assertEquals(1500, response.body!!.priceCents)
    }

    @Test
    fun `delete menu item returns 204`() {
        val createReq = MenuItemRequest(name = "To Delete", priceCents = 500)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            createReq,
            MenuItemResponse::class.java
        ).body!!

        val response = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu/${created.id}"),
            HttpMethod.DELETE,
            null,
            Void::class.java
        )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
    }

    @Test
    fun `toggle availability returns updated item`() {
        val createReq = MenuItemRequest(name = "Toggle Item", priceCents = 800)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            createReq,
            MenuItemResponse::class.java
        ).body!!

        assertTrue(created.available)

        val response = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu/${created.id}/availability?available=false"),
            HttpMethod.PATCH,
            null,
            MenuItemResponse::class.java
        )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
    }

    @Test
    fun `wrong restaurant cannot toggle another restaurants item`() {
        val createReq = MenuItemRequest(name = "Protected Item", priceCents = 1000)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            createReq,
            MenuItemResponse::class.java
        ).body!!

        val wrongRestaurantId = UUID.randomUUID()

        val response = restTemplate.exchange(
            url("/api/restaurants/$wrongRestaurantId/menu/${created.id}/availability?available=false"),
            HttpMethod.PATCH,
            null,
            String::class.java
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }
}
