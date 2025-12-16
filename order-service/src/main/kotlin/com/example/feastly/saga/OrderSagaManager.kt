package com.example.feastly.saga

import com.example.feastly.order.OrderRepository
import com.example.feastly.order.OrderStatus
import com.feastly.events.AssignDriverCommand
import com.feastly.events.DeliveryCompletedEvent
import com.feastly.events.DriverAssignedEvent
import com.feastly.events.DriverDeliveryFailedEvent
import com.feastly.events.KafkaTopics
import com.feastly.events.OrderPlacedEvent
import com.feastly.events.RestaurantOrderAcceptedEvent
import com.feastly.events.RestaurantOrderRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderSagaManager(
    private val orderRepository: OrderRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OrderSagaManager::class.java)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = "order-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun onOrderPlaced(message: String) {
        val event = objectMapper.readValue(message, OrderPlacedEvent::class.java)
        log.info("Saga received OrderPlacedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        // Idempotency: only process if order is in SUBMITTED state
        if (order.status != OrderStatus.SUBMITTED) {
            log.debug("Order {} already processed (status={}), skipping", event.orderId, order.status)
            return
        }

        // Emit RestaurantOrderRequest
        val request = RestaurantOrderRequest(
            orderId = order.id,
            restaurantId = order.restaurantId
        )
        kafkaTemplate.send(KafkaTopics.RESTAURANT_ORDER_REQUEST, order.id.toString(), request)
        log.info("Saga emitted RestaurantOrderRequest for order {}", order.id)
    }

    @KafkaListener(
        topics = [KafkaTopics.RESTAURANT_EVENTS],
        groupId = "order-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun onRestaurantOrderAccepted(message: String) {
        val event = objectMapper.readValue(message, RestaurantOrderAcceptedEvent::class.java)
        log.info("Saga received RestaurantOrderAcceptedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        // Idempotency: only process if order is in SUBMITTED or ACCEPTED state
        if (order.status != OrderStatus.SUBMITTED && order.status != OrderStatus.ACCEPTED) {
            log.debug("Order {} already past ACCEPTED (status={}), skipping", event.orderId, order.status)
            return
        }

        // Update status to ACCEPTED
        order.status = OrderStatus.ACCEPTED
        orderRepository.save(order)

        // Emit AssignDriverCommand
        val command = AssignDriverCommand(
            orderId = order.id,
            restaurantId = order.restaurantId
        )
        kafkaTemplate.send(KafkaTopics.DISPATCH_ASSIGN_DRIVER, order.id.toString(), command)
        log.info("Saga emitted AssignDriverCommand for order {}", order.id)
    }

    @KafkaListener(
        topics = [KafkaTopics.DISPATCH_EVENTS],
        groupId = "order-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun onDispatchEvent(message: String) {
        val json = objectMapper.readTree(message)
        val orderId = json.get("orderId")?.asText() ?: return

        // Determine event type based on fields present
        when {
            json.has("reason") -> handleDeliveryFailed(json)
            json.has("driverId") && !json.has("reason") -> {
                // Could be DriverAssignedEvent or DeliveryCompletedEvent
                // Check order status to determine which
                val order = orderRepository.findById(java.util.UUID.fromString(orderId)).orElse(null)
                if (order != null && order.status == OrderStatus.ACCEPTED) {
                    handleDriverAssigned(json)
                } else if (order != null && (order.status == OrderStatus.DRIVER_ASSIGNED || order.status == OrderStatus.DISPATCHED)) {
                    handleDeliveryCompleted(json)
                }
            }
        }
    }

    private fun handleDriverAssigned(json: JsonNode) {
        val event = objectMapper.treeToValue(json, DriverAssignedEvent::class.java)
        log.info("Saga handling DriverAssignedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        // Idempotency: only process if order is in ACCEPTED state
        if (order.status != OrderStatus.ACCEPTED) {
            log.debug("Order {} not in ACCEPTED state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.status = OrderStatus.DRIVER_ASSIGNED
        order.driverId = event.driverId
        orderRepository.save(order)
        log.info("Order {} updated to DRIVER_ASSIGNED with driver {}", order.id, event.driverId)
    }

    private fun handleDeliveryCompleted(json: JsonNode) {
        val event = objectMapper.treeToValue(json, DeliveryCompletedEvent::class.java)
        log.info("Saga handling DeliveryCompletedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        // Idempotency: only process if order is in DRIVER_ASSIGNED or DISPATCHED state
        if (order.status != OrderStatus.DRIVER_ASSIGNED && order.status != OrderStatus.DISPATCHED) {
            log.debug("Order {} not in expected state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.status = OrderStatus.DELIVERED
        orderRepository.save(order)
        log.info("Order {} updated to DELIVERED", order.id)
    }

    private fun handleDeliveryFailed(json: JsonNode) {
        val event = objectMapper.treeToValue(json, DriverDeliveryFailedEvent::class.java)
        log.info("Saga handling DriverDeliveryFailedEvent for order {}: {}", event.orderId, event.reason)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        // Idempotency: only process if order is not already in terminal state
        if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.DISPATCH_FAILED) {
            log.debug("Order {} already in terminal state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.status = OrderStatus.DISPATCH_FAILED
        orderRepository.save(order)
        log.info("Order {} updated to DISPATCH_FAILED", order.id)
    }
}
