package com.example.feastly.menu

import com.example.feastly.BaseIntegrationTest

import com.example.feastly.restaurant.RestaurantRegisterRequest
import com.example.feastly.restaurant.RestaurantResponse
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

    private var restaurantId: UUID? = null

    private fun url(path: String) = "http://localhost:$port$path"

    @BeforeEach
    fun setup() {
        val req = RestaurantRegisterRequest(
            name = "Test Restaurant ${UUID.randomUUID()}",
            address = "123 Test St",
            cuisine = "Italian"
        )
        val res = restTemplate.postForEntity(url("/api/restaurants"), req, RestaurantResponse::class.java)
        restaurantId = res.body!!.id
    }

    @Test
    fun `add menu item returns 201`() {
        val req = MenuItemRequest(
            name = "Margherita Pizza",
            description = "Classic tomato and mozzarella",
            priceCents = 1299,
            available = true
        )

        val res = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertNotNull(res.body)
        assertEquals("Margherita Pizza", res.body!!.name)
        assertEquals(1299, res.body!!.priceCents)
    }

    @Test
    fun `get menu returns list of items`() {
        val req = MenuItemRequest(
            name = "Spaghetti Carbonara",
            description = "Creamy pasta with bacon",
            priceCents = 1499
        )
        restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        )

        val res = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu"),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Array<MenuItemResponse>::class.java
        )

        assertEquals(HttpStatus.OK, res.statusCode)
        assertTrue(res.body!!.any { it.name == "Spaghetti Carbonara" })
    }

    @Test
    fun `update menu item returns updated item`() {
        val createReq = MenuItemRequest(name = "Tiramisu", priceCents = 799)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            createReq,
            MenuItemResponse::class.java
        ).body!!

        val updateReq = MenuItemRequest(name = "Tiramisu Deluxe", priceCents = 999)
        val updated = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu/${created.id}"),
            HttpMethod.PUT,
            HttpEntity(updateReq),
            MenuItemResponse::class.java
        )

        assertEquals(HttpStatus.OK, updated.statusCode)
        assertEquals("Tiramisu Deluxe", updated.body!!.name)
        assertEquals(999, updated.body!!.priceCents)
    }

    @Test
    fun `delete menu item returns 204`() {
        val req = MenuItemRequest(name = "Gelato", priceCents = 499)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        ).body!!

        val res = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu/${created.id}"),
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            Void::class.java
        )

        assertEquals(HttpStatus.NO_CONTENT, res.statusCode)
    }

    @Test
    fun `get menu for non-existent restaurant returns 404`() {
        val fakeRestaurantId = UUID.randomUUID()

        val res = restTemplate.exchange(
            url("/api/restaurants/$fakeRestaurantId/menu"),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode)
    }

    @Test
    fun `mark item unavailable and verify GET shows flag`() {
        val req = MenuItemRequest(name = "Pizza Special", priceCents = 1599, available = true)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        ).body!!
        assertTrue(created.available)

        val patchRes = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu/${created.id}/availability?available=false"),
            HttpMethod.PATCH,
            HttpEntity.EMPTY,
            Void::class.java
        )
        assertEquals(HttpStatus.NO_CONTENT, patchRes.statusCode)

        val menu = restTemplate.exchange(
            url("/api/restaurants/$restaurantId/menu"),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Array<MenuItemResponse>::class.java
        ).body!!

        val item = menu.find { it.id == created.id }
        assertNotNull(item)
        assertEquals(false, item!!.available)
    }

    @Test
    fun `wrong restaurant cannot toggle another restaurants item`() {
        val req = MenuItemRequest(name = "Exclusive Dish", priceCents = 2500)
        val created = restTemplate.postForEntity(
            url("/api/restaurants/$restaurantId/menu"),
            req,
            MenuItemResponse::class.java
        ).body!!

        val restaurant2Req = RestaurantRegisterRequest(
            name = "Other Restaurant ${UUID.randomUUID()}",
            address = "456 Other St",
            cuisine = "French"
        )
        val restaurant2 = restTemplate.postForEntity(
            url("/api/restaurants"),
            restaurant2Req,
            RestaurantResponse::class.java
        ).body!!

        val res = restTemplate.exchange(
            url("/api/restaurants/${restaurant2.id}/menu/${created.id}/availability?available=false"),
            HttpMethod.PATCH,
            HttpEntity.EMPTY,
            String::class.java
        )

        assertEquals(HttpStatus.FORBIDDEN, res.statusCode)
    }
}

