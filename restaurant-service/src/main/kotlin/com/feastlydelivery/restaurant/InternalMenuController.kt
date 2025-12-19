package com.feastlydelivery.restaurant

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal")
class InternalMenuController(private val menuItemRepository: MenuItemRepository) {

    private val logger = LoggerFactory.getLogger(InternalMenuController::class.java)

    @PostMapping("/menu-items:batchGet")
    fun batchGetMenuItems(@RequestBody request: BatchGetMenuItemsRequest): ResponseEntity<BatchGetMenuItemsResponse> {
        logger.debug("Batch fetching menu items: {}", request.ids)
        
        val items = menuItemRepository.findAllById(request.ids)
            .map { it.toInternalResponse() }

        return ResponseEntity.ok(BatchGetMenuItemsResponse(items = items))
    }
}

data class BatchGetMenuItemsRequest(
    val ids: List<UUID>
)

data class BatchGetMenuItemsResponse(
    val items: List<InternalMenuItemResponse>
)

data class InternalMenuItemResponse(
    val id: UUID,
    val restaurantId: UUID,
    val priceCents: Int,
    val available: Boolean,
    val name: String
)

fun MenuItem.toInternalResponse() = InternalMenuItemResponse(
    id = id,
    restaurantId = restaurantId,
    priceCents = priceCents,
    available = available,
    name = name
)
