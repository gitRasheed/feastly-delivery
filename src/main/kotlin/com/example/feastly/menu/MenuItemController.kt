package com.example.feastly.menu

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menu")
class MenuItemController(
    private val service: MenuItemService
) {

    @PostMapping
    fun addMenuItem(
        @PathVariable restaurantId: UUID,
        @Valid @RequestBody request: MenuItemRequest
    ): ResponseEntity<MenuItemResponse> {
        val saved = service.addMenuItem(restaurantId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping
    fun getMenu(@PathVariable restaurantId: UUID): List<MenuItemResponse> =
        service.getMenuByRestaurant(restaurantId).map { it.toResponse() }

    @PutMapping("/{menuItemId}")
    fun updateMenuItem(
        @PathVariable restaurantId: UUID,
        @PathVariable menuItemId: UUID,
        @Valid @RequestBody request: MenuItemRequest
    ): MenuItemResponse {
        val updated = service.updateMenuItem(restaurantId, menuItemId, request)
        return updated.toResponse()
    }

    @DeleteMapping("/{menuItemId}")
    fun deleteMenuItem(
        @PathVariable restaurantId: UUID,
        @PathVariable menuItemId: UUID
    ): ResponseEntity<Void> {
        service.deleteMenuItem(restaurantId, menuItemId)
        return ResponseEntity.noContent().build()
    }
}
