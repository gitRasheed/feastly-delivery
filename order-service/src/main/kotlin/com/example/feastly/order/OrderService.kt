package com.example.feastly.order

import com.example.feastly.client.RestaurantClient
import com.example.feastly.client.RestaurantMenuClient
import com.example.feastly.client.UserClient
import com.example.feastly.common.DriverAlreadyAssignedException
import com.example.feastly.common.InvalidDeliveryStateException
import com.example.feastly.common.InvalidOrderStateForDispatchException
import com.example.feastly.common.MenuItemNotFoundException
import com.example.feastly.common.MenuItemUnavailableException
import com.example.feastly.common.OrderAlreadyDeliveredException
import com.example.feastly.common.OrderAlreadyFinalizedException
import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.common.RestaurantNotFoundException
import com.example.feastly.common.UnauthorizedDriverAccessException
import com.example.feastly.common.UnauthorizedRestaurantAccessException
import com.example.feastly.common.UserNotFoundException
import com.example.feastly.events.OrderAcceptedEvent
import com.example.feastly.outbox.OrderEventFactory
import com.example.feastly.outbox.OrderEventType
import com.example.feastly.outbox.OutboxEntry
import com.example.feastly.outbox.OutboxRepository
import com.example.feastly.payment.PaymentService
import com.example.feastly.pricing.PricingService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for order lifecycle management.
 *
 * ## Service Dependency Model
 *
 * **Write-time dependencies (order creation):**
 * - [UserClient]: Validates customer exists
 * - [RestaurantClient]: Validates restaurant exists
 * - [RestaurantMenuClient]: Fetches menu items for validation and price/name snapshotting
 *
 * **Read-time dependencies (order retrieval):**
 * - Database only. No external service calls.
 *
 * This design ensures that **order reads are always available**, even when
 * restaurant-service or other upstream services are unavailable. All required
 * data (menu item names, prices) is snapshotted into [OrderItem] at creation time.
 */
@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userClient: UserClient,
    private val restaurantClient: RestaurantClient,
    private val restaurantMenuClient: RestaurantMenuClient,
    private val paymentService: PaymentService,
    private val pricingService: PricingService,
    private val outboxRepository: OutboxRepository,
    private val orderEventFactory: OrderEventFactory,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    companion object {
        private const val ORDER_EVENTS_TOPIC = "order.events"
    }

    fun create(userId: UUID, request: CreateOrderRequest): DeliveryOrder {
        if (!userClient.existsById(userId)) {
            throw UserNotFoundException(userId)
        }

        if (!restaurantClient.existsById(request.restaurantId)) {
            throw RestaurantNotFoundException(request.restaurantId)
        }


        val menuItemIds = request.items.map { it.menuItemId }
        val fetchedItems = restaurantMenuClient.batchGetMenuItems(menuItemIds)
        val menuItemsById = fetchedItems.associateBy { it.id }


        val menuItems = request.items.map { itemRequest ->
            val menuItem = menuItemsById[itemRequest.menuItemId]
                ?: throw MenuItemNotFoundException(itemRequest.menuItemId)

            require(menuItem.restaurantId == request.restaurantId) {
                "Menu item ${menuItem.id} does not belong to restaurant ${request.restaurantId}"
            }

            if (!menuItem.available) {
                throw MenuItemUnavailableException(menuItem.id)
            }

            menuItem to itemRequest.quantity
        }


        val order = DeliveryOrder(
            customerId = userId,
            restaurantId = request.restaurantId,
            driverId = request.driverId,
            status = OrderStatus.SUBMITTED.name
        )

        val savedOrder = orderRepository.save(order)

        val orderItems = menuItems.map { (menuItem, quantity) ->
            OrderItem(
                orderId = savedOrder.id,
                menuItemId = menuItem.id,
                menuItemName = menuItem.name,
                quantity = quantity,
                priceCents = menuItem.priceCents
            )
        }
        orderItemRepository.saveAll(orderItems)

        val breakdown = pricingService.priceExistingOrder(
            orderId = savedOrder.id,
            discountCode = request.discountCode,
            tipCents = request.tipCents ?: 0
        )

        savedOrder.subtotalCents = breakdown.itemsSubtotalCents
        savedOrder.taxCents = breakdown.serviceFeeCents
        savedOrder.deliveryFeeCents = breakdown.deliveryFeeCents
        savedOrder.totalCents = breakdown.totalCents

        val finalOrder = orderRepository.save(savedOrder)

        val eventPayload = orderEventFactory.buildEventPayload(
            eventType = OrderEventType.ORDER_SUBMITTED,
            order = finalOrder,
            items = orderItems
        )
        outboxRepository.save(OutboxEntry(
            aggregateId = finalOrder.id,
            aggregateType = "Order",
            eventType = OrderEventType.ORDER_SUBMITTED.name,
            payload = eventPayload
        ))

        return finalOrder
    }

    fun updateStatus(orderId: UUID, newStatus: OrderStatus): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        requireNotTerminal(order)

        order.setOrderStatus(newStatus)

        return orderRepository.save(order)
    }

    fun acceptOrder(restaurantId: UUID, orderId: UUID): DeliveryOrder {
        val order = findOrderAndValidateRestaurant(restaurantId, orderId)
        requireSubmittedStatus(order)

        order.setOrderStatus(OrderStatus.ACCEPTED)

        val saved = orderRepository.save(order)

        val event = OrderAcceptedEvent(
            orderId = saved.id,
            restaurantId = restaurantId
        )
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, saved.id.toString(), event)

        return saved
    }

    fun rejectOrder(restaurantId: UUID, orderId: UUID): DeliveryOrder {
        val order = findOrderAndValidateRestaurant(restaurantId, orderId)
        requireSubmittedStatus(order)

        order.setOrderStatus(OrderStatus.CANCELLED)

        return orderRepository.save(order)
    }

    fun assignDriver(orderId: UUID, driverId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        requireNotTerminal(order)

        if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw InvalidOrderStateForDispatchException(orderId, order.getOrderStatus())
        }

        if (order.driverId != null) {
            throw DriverAlreadyAssignedException(orderId)
        }

        order.driverId = driverId

        return orderRepository.save(order)
    }

    fun confirmPickup(orderId: UUID, driverId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        requireNotTerminal(order)

        if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw InvalidOrderStateForDispatchException(orderId, order.getOrderStatus())
        }

        if (order.driverId != driverId) {
            throw UnauthorizedDriverAccessException(driverId)
        }

        order.setOrderStatus(OrderStatus.DISPATCHED)

        return orderRepository.save(order)
    }

    fun confirmDelivery(orderId: UUID, driverId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        if (order.getOrderStatus() != OrderStatus.DISPATCHED) {
            throw InvalidDeliveryStateException(orderId, order.getOrderStatus())
        }

        if (order.driverId != driverId) {
            throw UnauthorizedDriverAccessException(driverId)
        }

        order.setOrderStatus(OrderStatus.DELIVERED)

        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getOrdersForUser(userId: UUID): List<DeliveryOrder> {
        if (!userClient.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
        return orderRepository.findByCustomerId(userId)
    }

    fun refundOrder(orderId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        val refundResult = paymentService.refundOrder(orderId)

        return orderRepository.save(order)
    }

    private fun findOrderAndValidateRestaurant(restaurantId: UUID, orderId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        if (order.restaurantId != restaurantId) {
            throw UnauthorizedRestaurantAccessException(restaurantId)
        }

        return order
    }

    private fun requireSubmittedStatus(order: DeliveryOrder) {
        if (order.getOrderStatus() != OrderStatus.SUBMITTED) {
            throw OrderAlreadyFinalizedException(order.getOrderStatus())
        }
    }

    private fun requireNotTerminal(order: DeliveryOrder) {
        val status = order.getOrderStatus()
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw OrderAlreadyDeliveredException(order.id)
        }
    }
}
