package com.feastlydelivery.restaurant.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.feastlydelivery.restaurant.RestaurantRepository
import com.feastlydelivery.restaurant.config.KafkaTopics
import com.feastlydelivery.restaurant.events.RestaurantOrderAcceptedEvent
import com.feastlydelivery.restaurant.events.RestaurantOrderRejectedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RestaurantEventListener(
    private val restaurantRepository: RestaurantRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(RestaurantEventListener::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    @KafkaListener(
        topics = [KafkaTopics.RESTAURANT_ORDER_REQUEST],
        groupId = "restaurant-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleOrderRequest(record: ConsumerRecord<String, Any>) {
        val event = record.value()
        
        when (event) {
            is Map<*, *> -> processOrderRequest(event)
            else -> logger.debug("Ignoring event type: ${event::class.simpleName}")
        }
    }

    private fun processOrderRequest(eventMap: Map<*, *>) {
        val orderId = extractUUID(eventMap, "orderId") ?: return
        val restaurantId = extractUUID(eventMap, "restaurantId") ?: return
        
        logger.info("Processing RestaurantOrderRequest for order {} restaurant {}", orderId, restaurantId)

        val validationResult = validateRestaurant(restaurantId)
        
        when (validationResult) {
            is ValidationResult.Valid -> emitAccepted(orderId, restaurantId)
            is ValidationResult.Invalid -> emitRejected(orderId, restaurantId, validationResult.reason)
        }
    }

    private fun extractUUID(map: Map<*, *>, key: String): UUID? {
        val value = map[key] ?: run {
            logger.warn("Missing required field: {}", key)
            return null
        }
        return try {
            UUID.fromString(value.toString())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID for field {}: {}", key, value)
            null
        }
    }

    private fun validateRestaurant(restaurantId: UUID): ValidationResult {
        val restaurant = restaurantRepository.findById(restaurantId).orElse(null)
            ?: return ValidationResult.Invalid("Restaurant not found")
        
        if (!restaurant.isOpen) {
            return ValidationResult.Invalid("Restaurant is closed")
        }
        
        return ValidationResult.Valid
    }

    private fun emitAccepted(orderId: UUID, restaurantId: UUID) {
        val event = RestaurantOrderAcceptedEvent(orderId = orderId, restaurantId = restaurantId)
        kafkaTemplate.send(KafkaTopics.RESTAURANT_EVENTS, orderId.toString(), event)
        logger.info("Emitted RestaurantOrderAcceptedEvent for order {}", orderId)
    }

    private fun emitRejected(orderId: UUID, restaurantId: UUID, reason: String) {
        val event = RestaurantOrderRejectedEvent(orderId = orderId, restaurantId = restaurantId, reason = reason)
        kafkaTemplate.send(KafkaTopics.RESTAURANT_EVENTS, orderId.toString(), event)
        logger.info("Emitted RestaurantOrderRejectedEvent for order {}: {}", orderId, reason)
    }

    private sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
