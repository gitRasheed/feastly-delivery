package com.example.feastly.saga

import com.example.feastly.order.OrderRepository
import com.example.feastly.order.OrderStatus
import com.example.feastly.events.AssignDriverCommand
import com.example.feastly.events.DeliveryCompletedEvent
import com.example.feastly.events.DriverAssignedEvent
import com.example.feastly.events.DriverDeliveryFailedEvent
import com.example.feastly.events.KafkaTopics
import com.example.feastly.events.OrderPlacedEvent
import com.example.feastly.events.RestaurantOrderAcceptedEvent
import com.example.feastly.events.RestaurantOrderRejectedEvent
import com.example.feastly.events.RestaurantOrderRequest
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderSagaManager(
    private val orderRepository: OrderRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(OrderSagaManager::class.java)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = "order-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun onOrderEvent(event: Any) {
        when (event) {
            is OrderPlacedEvent -> handleOrderPlaced(event)
            else -> log.debug("Ignoring event type on order.events: {}", event::class.simpleName)
        }
    }

    private fun handleOrderPlaced(event: OrderPlacedEvent) {
        log.info("Saga received OrderPlacedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        if (order.getOrderStatus() != OrderStatus.SUBMITTED) {
            log.debug("Order {} already processed (status={}), skipping", event.orderId, order.status)
            return
        }

        val request = RestaurantOrderRequest(orderId = order.id, restaurantId = order.restaurantId)
        kafkaTemplate.send(KafkaTopics.RESTAURANT_ORDER_REQUEST, order.id.toString(), request)
        log.info("Saga emitted RestaurantOrderRequest for order {}", order.id)
    }

    @KafkaListener(
        topics = [KafkaTopics.RESTAURANT_EVENTS],
        groupId = "order-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun onRestaurantEvent(event: Any) {
        when (event) {
            is RestaurantOrderAcceptedEvent -> handleRestaurantOrderAccepted(event)
            is RestaurantOrderRejectedEvent -> handleRestaurantOrderRejected(event)
            else -> log.debug("Ignoring event type on restaurant.events: {}", event::class.simpleName)
        }
    }

    private fun handleRestaurantOrderAccepted(event: RestaurantOrderAcceptedEvent) {
        log.info("Saga received RestaurantOrderAcceptedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        val status = order.getOrderStatus()
        if (status != OrderStatus.SUBMITTED && status != OrderStatus.ACCEPTED) {
            log.debug("Order {} already past ACCEPTED (status={}), skipping", event.orderId, order.status)
            return
        }

        order.setOrderStatus(OrderStatus.AWAITING_DRIVER)
        orderRepository.save(order)

        val command = AssignDriverCommand(orderId = order.id, restaurantId = order.restaurantId)
        kafkaTemplate.send(KafkaTopics.DISPATCH_ASSIGN_DRIVER, order.id.toString(), command)
        log.info("Saga emitted AssignDriverCommand for order {}", order.id)
    }

    private fun handleRestaurantOrderRejected(event: RestaurantOrderRejectedEvent) {
        log.info("Saga received RestaurantOrderRejectedEvent for order {}: {}", event.orderId, event.reason)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.debug("Order {} already CANCELLED, skipping", event.orderId)
            return
        }

        order.setOrderStatus(OrderStatus.CANCELLED)
        orderRepository.save(order)
        log.info("Order {} marked as CANCELLED due to restaurant rejection", order.id)
    }

    @KafkaListener(
        topics = [KafkaTopics.DISPATCH_EVENTS],
        groupId = "order-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    fun onDispatchEvent(event: Any) {
        when (event) {
            is DriverAssignedEvent -> handleDriverAssigned(event)
            is DeliveryCompletedEvent -> handleDeliveryCompleted(event)
            is DriverDeliveryFailedEvent -> handleDeliveryFailed(event)
            else -> log.debug("Ignoring event type on dispatch.events: {}", event::class.simpleName)
        }
    }

    private fun handleDriverAssigned(event: DriverAssignedEvent) {
        log.info("Saga handling DriverAssignedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        if (order.getOrderStatus() != OrderStatus.AWAITING_DRIVER) {
            log.debug("Order {} not in AWAITING_DRIVER state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.setOrderStatus(OrderStatus.DRIVER_ASSIGNED)
        order.driverId = event.driverId
        orderRepository.save(order)
        log.info("Order {} updated to DRIVER_ASSIGNED with driver {}", order.id, event.driverId)
    }

    private fun handleDeliveryCompleted(event: DeliveryCompletedEvent) {
        log.info("Saga handling DeliveryCompletedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        val status = order.getOrderStatus()
        if (status != OrderStatus.DRIVER_ASSIGNED && status != OrderStatus.DISPATCHED) {
            log.debug("Order {} not in expected state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.setOrderStatus(OrderStatus.DELIVERED)
        orderRepository.save(order)
        log.info("Order {} updated to DELIVERED", order.id)
    }

    private fun handleDeliveryFailed(event: DriverDeliveryFailedEvent) {
        log.info("Saga handling DriverDeliveryFailedEvent for order {}: {}", event.orderId, event.reason)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        val status = order.getOrderStatus()
        if (status == OrderStatus.DELIVERED || status == OrderStatus.DISPATCH_FAILED) {
            log.debug("Order {} already in terminal state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.setOrderStatus(OrderStatus.DISPATCH_FAILED)
        orderRepository.save(order)
        log.info("Order {} updated to DISPATCH_FAILED", order.id)
    }
}
