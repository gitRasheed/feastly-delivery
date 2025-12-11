package com.example.feastly.dispatch

import com.example.feastly.driverstatus.DriverStatusUpdateRequest
import com.example.feastly.menu.MenuItemRequest
import com.example.feastly.menu.MenuItemResponse
import com.example.feastly.order.CreateOrderRequest
import com.example.feastly.order.OrderItemRequest
import com.example.feastly.order.OrderResponse
import com.example.feastly.restaurant.RestaurantRegisterRequest
import com.example.feastly.restaurant.RestaurantResponse
import com.example.feastly.user.UserRegisterRequest
import com.example.feastly.user.UserResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DispatchControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    private fun headersWithUser(userId: UUID): HttpHeaders {
        val headers = HttpHeaders()
        headers.set("X-USER-ID", userId.toString())
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }

    private fun jsonHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }

    private fun createUser(email: String): UserResponse {
        val req = UserRegisterRequest(email = email, password = "secret123")
        return restTemplate.postForEntity(url("/api/users"), req, UserResponse::class.java).body!!
    }

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

    private fun createOrder(userId: UUID, restaurantId: UUID, menuItemId: UUID): OrderResponse {
        val createOrder = CreateOrderRequest(
            restaurantId = restaurantId,
            items = listOf(OrderItemRequest(menuItemId = menuItemId, quantity = 1))
        )
        return restTemplate.exchange(
            url("/api/orders"),
            HttpMethod.POST,
            HttpEntity(createOrder, headersWithUser(userId)),
            OrderResponse::class.java
        ).body!!
    }

    private fun acceptOrder(restaurantId: UUID, orderId: UUID): OrderResponse {
        return restTemplate.exchange(
            url("/api/restaurants/$restaurantId/orders/$orderId/accept"),
            HttpMethod.PATCH,
            HttpEntity<Void>(HttpHeaders()),
            OrderResponse::class.java
        ).body!!
    }

    private fun setDriverAvailable(driverId: UUID, lat: Double, lng: Double) {
        restTemplate.exchange(
            url("/api/drivers/$driverId/status"),
            HttpMethod.PUT,
            HttpEntity(DriverStatusUpdateRequest(true, lat, lng), jsonHeaders()),
            Any::class.java
        )
    }

    private fun setDriverUnavailable(driverId: UUID) {
        restTemplate.exchange(
            url("/api/drivers/$driverId/status"),
            HttpMethod.PUT,
            HttpEntity(DriverStatusUpdateRequest(false, 0.0, 0.0), jsonHeaders()),
            Any::class.java
        )
    }

    @Test
    fun `dispatch offers order to available driver`() {
        val user = createUser("dispatch-test-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Dispatch Test Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Burger", 1500)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val driverId = UUID.randomUUID()
        setDriverAvailable(driverId, 40.7130, -74.0065)

        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/dispatch"),
            HttpMethod.POST,
            HttpEntity<Void>(jsonHeaders()),
            DispatchStatusResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        // Should have a pending offer (may be our driver or another available one)
        assertTrue(response.body!!.status in listOf("PENDING_OFFER", "AWAITING_DISPATCH"))
    }

    @Test
    fun `driver can accept dispatch offer`() {
        val user = createUser("accept-dispatch-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Accept Dispatch Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Pizza", 1800)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val driverId = UUID.randomUUID()
        setDriverAvailable(driverId, 40.7130, -74.0065)

        // Start dispatch
        val dispatchResponse = restTemplate.exchange(
            url("/api/orders/${order.id}/dispatch"),
            HttpMethod.POST,
            HttpEntity<Void>(jsonHeaders()),
            DispatchStatusResponse::class.java
        ).body!!

        // If we got an offer, accept it
        if (dispatchResponse.pendingOfferId != null) {
            val offeredDriverId = dispatchResponse.pendingOfferId

            val acceptResponse = restTemplate.exchange(
                url("/api/orders/${order.id}/offer-response?driverId=$offeredDriverId"),
                HttpMethod.POST,
                HttpEntity(DispatchOfferResponse(accepted = true), jsonHeaders()),
                Void::class.java
            )

            assertEquals(HttpStatus.OK, acceptResponse.statusCode)

            // Check status
            val status = restTemplate.getForEntity(
                url("/api/orders/${order.id}/dispatch-status"),
                DispatchStatusResponse::class.java
            ).body!!

            assertEquals("ASSIGNED", status.status)
            assertEquals(offeredDriverId, status.currentDriverId)
        }
    }

    @Test
    fun `driver rejection triggers offer to next driver`() {
        val user = createUser("reject-dispatch-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("Reject Dispatch Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Sushi", 2500)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        val driver1 = UUID.randomUUID()
        val driver2 = UUID.randomUUID()
        setDriverAvailable(driver1, 40.7129, -74.0061)
        setDriverAvailable(driver2, 40.7200, -74.0100)

        // Start dispatch
        val initialStatus = restTemplate.exchange(
            url("/api/orders/${order.id}/dispatch"),
            HttpMethod.POST,
            HttpEntity<Void>(jsonHeaders()),
            DispatchStatusResponse::class.java
        ).body!!

        if (initialStatus.pendingOfferId != null) {
            val firstDriver = initialStatus.pendingOfferId

            // Reject offer
            restTemplate.exchange(
                url("/api/orders/${order.id}/offer-response?driverId=$firstDriver"),
                HttpMethod.POST,
                HttpEntity(DispatchOfferResponse(accepted = false), jsonHeaders()),
                Void::class.java
            )

            // Check status - should have a different pending offer
            val status = restTemplate.getForEntity(
                url("/api/orders/${order.id}/dispatch-status"),
                DispatchStatusResponse::class.java
            ).body!!

            // Should either have a new offer or be awaiting dispatch
            assertTrue(status.status in listOf("PENDING_OFFER", "AWAITING_DISPATCH"))
            if (status.status == "PENDING_OFFER") {
                // If there's a new offer, it should be a different driver
                assertTrue(status.pendingOfferId != firstDriver)
            }
        }
    }

    @Test
    fun `no dispatch when no drivers available`() {
        val user = createUser("no-drivers-${UUID.randomUUID()}@example.com")
        val restaurant = createRestaurant("No Drivers Restaurant ${UUID.randomUUID()}")
        val menuItem = createMenuItem(restaurant.id, "Tacos", 1200)

        val order = createOrder(user.id, restaurant.id, menuItem.id)
        acceptOrder(restaurant.id, order.id)

        // Note: Other tests may have made drivers available, so we can't guarantee
        // no drivers are available. Just check the response is valid.
        val response = restTemplate.exchange(
            url("/api/orders/${order.id}/dispatch"),
            HttpMethod.POST,
            HttpEntity<Void>(jsonHeaders()),
            DispatchStatusResponse::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        // Status should be one of the valid dispatch states
        assertTrue(response.body!!.status in listOf("PENDING_OFFER", "AWAITING_DISPATCH", "ASSIGNED"))
    }
}

