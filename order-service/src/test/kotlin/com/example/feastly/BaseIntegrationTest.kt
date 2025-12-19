package com.example.feastly

import com.example.feastly.client.MenuItemData
import com.example.feastly.client.RestaurantMenuClient
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class BaseIntegrationTest {

    @MockBean
    protected lateinit var kafkaTemplate: KafkaTemplate<String, Any>
}

/**
 * Test configuration for RestaurantMenuClient that stores menu items in memory.
 * Tests can register menu items using the companion object methods.
 */
@Component
@Primary
class TestRestaurantMenuClient : RestaurantMenuClient {

    companion object {
        private val menuItems = ConcurrentHashMap<UUID, MenuItemData>()

        fun registerMenuItem(
            id: UUID = UUID.randomUUID(),
            restaurantId: UUID,
            name: String,
            priceCents: Int,
            available: Boolean = true
        ): MenuItemData {
            val item = MenuItemData(
                id = id,
                restaurantId = restaurantId,
                priceCents = priceCents,
                available = available,
                name = name
            )
            menuItems[id] = item
            return item
        }

        fun clearMenuItems() {
            menuItems.clear()
        }

        fun setAvailability(itemId: UUID, available: Boolean) {
            menuItems[itemId]?.let { item ->
                menuItems[itemId] = item.copy(available = available)
            }
        }
    }

    override fun batchGetMenuItems(ids: List<UUID>): List<MenuItemData> {
        return ids.mapNotNull { menuItems[it] }
    }
}
