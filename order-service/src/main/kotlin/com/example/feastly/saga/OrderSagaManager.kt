package com.example.feastly.saga

import com.example.feastly.order.OrderRepository
import com.example.feastly.order.OrderStatus
import com.feastly.events.AssignDriverCommand
import com.feastly.events.DriverAssignedEvent
import com.feastly.events.KafkaTopics
import com.feastly.events.OrderPlacedEvent
import com.feastly.events.RestaurantOrderAcceptedEvent
import com.feastly.events.RestaurantOrderRequest
import com.fasterxml.jackson.databind.ObjectMapper
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
    fun onDriverAssigned(message: String) {
        val event = objectMapper.readValue(message, DriverAssignedEvent::class.java)
        log.info("Saga received DriverAssignedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        // Idempotency: only process if order is in ACCEPTED state
        if (order.status != OrderStatus.ACCEPTED) {
            log.debug("Order {} not in ACCEPTED state (status={}), skipping", event.orderId, order.status)
            return
        }

        // Update status and assign driver
        order.status = OrderStatus.DRIVER_ASSIGNED
        order.driverId = event.driverId
        orderRepository.save(order)
        log.info("Order {} updated to DRIVER_ASSIGNED with driver {}", order.id, event.driverId)
    }
}
