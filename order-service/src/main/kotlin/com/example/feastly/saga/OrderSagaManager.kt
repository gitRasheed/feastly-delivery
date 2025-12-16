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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Component
class OrderSagaManager(
    private val orderRepository: OrderRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OrderSagaManager::class.java)

    companion object {
        private val DISPATCH_TIMEOUT = Duration.ofSeconds(120)
        private const val MAX_DISPATCH_ATTEMPTS = 2
    }

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

        if (order.status != OrderStatus.SUBMITTED) {
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
    fun onRestaurantOrderAccepted(message: String) {
        val event = objectMapper.readValue(message, RestaurantOrderAcceptedEvent::class.java)
        log.info("Saga received RestaurantOrderAcceptedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order {} not found for saga processing", event.orderId)
            return
        }

        if (order.status != OrderStatus.SUBMITTED && order.status != OrderStatus.ACCEPTED) {
            log.debug("Order {} already past ACCEPTED (status={}), skipping", event.orderId, order.status)
            return
        }


        order.status = OrderStatus.AWAITING_DRIVER
        order.dispatchAttemptCount = 1
        order.dispatchSentAt = Instant.now()
        orderRepository.save(order)

        val command = AssignDriverCommand(orderId = order.id, restaurantId = order.restaurantId)
        kafkaTemplate.send(KafkaTopics.DISPATCH_ASSIGN_DRIVER, order.id.toString(), command)
        log.info("Saga emitted AssignDriverCommand for order {} (attempt {})", order.id, order.dispatchAttemptCount)
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    fun checkDispatchTimeouts() {
        val awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_DRIVER)
        val now = Instant.now()

        for (order in awaitingOrders) {
            val sentAt = order.dispatchSentAt ?: continue
            val elapsed = Duration.between(sentAt, now)

            if (elapsed >= DISPATCH_TIMEOUT) {
                if (order.dispatchAttemptCount < MAX_DISPATCH_ATTEMPTS) {
                    order.dispatchAttemptCount++
                    order.dispatchSentAt = now
                    orderRepository.save(order)

                    val command = AssignDriverCommand(orderId = order.id, restaurantId = order.restaurantId)
                    kafkaTemplate.send(KafkaTopics.DISPATCH_ASSIGN_DRIVER, order.id.toString(), command)
                    log.info("Saga retrying AssignDriverCommand for order {} (attempt {})", order.id, order.dispatchAttemptCount)
                } else {
                    order.status = OrderStatus.DISPATCH_FAILED
                    orderRepository.save(order)
                    log.warn("Order {} marked DISPATCH_FAILED after {} attempts", order.id, order.dispatchAttemptCount)
                }
            }
        }
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

        when {
            json.has("reason") -> handleDeliveryFailed(json)
            json.has("driverId") && !json.has("reason") -> {
                val order = orderRepository.findById(java.util.UUID.fromString(orderId)).orElse(null)
                when (order?.status) {
                    OrderStatus.AWAITING_DRIVER -> handleDriverAssigned(json)
                    OrderStatus.DRIVER_ASSIGNED, OrderStatus.DISPATCHED -> handleDeliveryCompleted(json)
                    else -> {}
                }
            }
        }
    }

    private fun handleDriverAssigned(json: JsonNode) {
        val event = objectMapper.treeToValue(json, DriverAssignedEvent::class.java)
        log.info("Saga handling DriverAssignedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        if (order.status != OrderStatus.AWAITING_DRIVER) {
            log.debug("Order {} not in AWAITING_DRIVER state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.status = OrderStatus.DRIVER_ASSIGNED
        order.driverId = event.driverId
        order.dispatchSentAt = null
        orderRepository.save(order)
        log.info("Order {} updated to DRIVER_ASSIGNED with driver {}", order.id, event.driverId)
    }

    private fun handleDeliveryCompleted(json: JsonNode) {
        val event = objectMapper.treeToValue(json, DeliveryCompletedEvent::class.java)
        log.info("Saga handling DeliveryCompletedEvent for order {}", event.orderId)

        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

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

        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.DISPATCH_FAILED) {
            log.debug("Order {} already in terminal state (status={}), skipping", event.orderId, order.status)
            return
        }

        order.status = OrderStatus.DISPATCH_FAILED
        orderRepository.save(order)
        log.info("Order {} updated to DISPATCH_FAILED", order.id)
    }
}
