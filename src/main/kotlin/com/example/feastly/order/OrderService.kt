package com.example.feastly.order

import com.example.feastly.common.DriverAlreadyAssignedException
import com.example.feastly.common.InvalidDeliveryStateException
import com.example.feastly.common.InvalidOrderStateForDispatchException
import com.example.feastly.common.MenuItemNotFoundException
import com.example.feastly.common.MenuItemUnavailableException
import com.example.feastly.common.OrderAlreadyDeliveredException
import com.example.feastly.common.OrderAlreadyFinalizedException
import com.example.feastly.common.OrderNotFoundException
import com.example.feastly.common.PaymentFailedException
import com.example.feastly.common.RefundNotAllowedException
import com.example.feastly.common.RestaurantNotFoundException
import com.example.feastly.common.UnauthorizedDriverAccessException
import com.example.feastly.common.UnauthorizedRestaurantAccessException
import com.example.feastly.common.UserNotFoundException
import com.example.feastly.menu.MenuItemRepository
import com.example.feastly.payment.PaymentService
import com.example.feastly.payment.PaymentStatus
import com.example.feastly.restaurant.RestaurantRepository
import com.example.feastly.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import com.example.feastly.pricing.PricingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val restaurantRepository: RestaurantRepository,
    private val menuItemRepository: MenuItemRepository,
    private val paymentService: PaymentService,
    private val pricingService: PricingService
) {

    fun create(userId: UUID, request: CreateOrderRequest): DeliveryOrder {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val restaurant = restaurantRepository.findByIdOrNull(request.restaurantId)
            ?: throw RestaurantNotFoundException(request.restaurantId)

        val menuItems = request.items.map { itemRequest ->
            val menuItem = menuItemRepository.findByIdOrNull(itemRequest.menuItemId)
                ?: throw MenuItemNotFoundException(itemRequest.menuItemId)

            require(menuItem.restaurant.id == request.restaurantId) {
                "Menu item ${menuItem.id} does not belong to restaurant ${request.restaurantId}"
            }

            if (!menuItem.available) {
                throw MenuItemUnavailableException(menuItem.id)
            }

            menuItem to itemRequest.quantity
        }

        val order = DeliveryOrder(
            user = user,
            restaurant = restaurant,
            driverId = request.driverId,
            status = OrderStatus.SUBMITTED,
            paymentStatus = PaymentStatus.PENDING
        )

        val orderItems = menuItems.map { (menuItem, quantity) ->
            OrderItem(
                order = order,
                menuItem = menuItem,
                quantity = quantity,
                priceCents = menuItem.priceCents
            )
        }
        order.items.addAll(orderItems)

        val savedOrder = orderRepository.save(order)

        val breakdown = pricingService.priceExistingOrder(
            orderId = savedOrder.id,
            discountCode = request.discountCode,
            tipCents = request.tipCents ?: 0
        )

        savedOrder.itemsSubtotalCents = breakdown.itemsSubtotalCents
        savedOrder.serviceFeeCents = breakdown.serviceFeeCents
        savedOrder.deliveryFeeCents = breakdown.deliveryFeeCents
        savedOrder.discountCents = breakdown.discountCents
        savedOrder.tipCents = breakdown.tipCents
        savedOrder.totalCents = breakdown.totalCents
        savedOrder.updatedAt = Instant.now()

        val paymentResult = paymentService.chargeOrder(
            customerId = userId,
            orderId = savedOrder.id,
            amountCents = breakdown.totalCents
        )

        if (paymentResult.success) {
            savedOrder.paymentStatus = PaymentStatus.PAID
            savedOrder.paymentReference = paymentResult.providerPaymentId
            savedOrder.updatedAt = Instant.now()
        } else {
            savedOrder.paymentStatus = PaymentStatus.FAILED
            savedOrder.updatedAt = Instant.now()
            orderRepository.save(savedOrder)
            throw PaymentFailedException(paymentResult.message)
        }

        return orderRepository.save(savedOrder)
    }

    fun updateStatus(orderId: UUID, newStatus: OrderStatus): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        requireNotTerminal(order)

        order.status = newStatus
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    fun acceptOrder(restaurantId: UUID, orderId: UUID): DeliveryOrder {
        val order = findOrderAndValidateRestaurant(restaurantId, orderId)
        requireSubmittedStatus(order)

        order.status = OrderStatus.ACCEPTED
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    fun rejectOrder(restaurantId: UUID, orderId: UUID): DeliveryOrder {
        val order = findOrderAndValidateRestaurant(restaurantId, orderId)
        requireSubmittedStatus(order)

        order.status = OrderStatus.CANCELLED
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    fun assignDriver(orderId: UUID, driverId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        requireNotTerminal(order)

        if (order.status != OrderStatus.ACCEPTED) {
            throw InvalidOrderStateForDispatchException(orderId, order.status)
        }

        if (order.driverId != null) {
            throw DriverAlreadyAssignedException(orderId)
        }

        order.driverId = driverId
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    fun confirmPickup(orderId: UUID, driverId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        requireNotTerminal(order)

        if (order.status != OrderStatus.ACCEPTED) {
            throw InvalidOrderStateForDispatchException(orderId, order.status)
        }

        if (order.driverId != driverId) {
            throw UnauthorizedDriverAccessException(driverId)
        }

        order.status = OrderStatus.DISPATCHED
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    fun confirmDelivery(orderId: UUID, driverId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        if (order.status != OrderStatus.DISPATCHED) {
            throw InvalidDeliveryStateException(orderId, order.status)
        }

        if (order.driverId != driverId) {
            throw UnauthorizedDriverAccessException(driverId)
        }

        order.status = OrderStatus.DELIVERED
        order.updatedAt = Instant.now()

        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getOrdersForUser(userId: UUID): List<DeliveryOrder> {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
        return orderRepository.findByUser_Id(userId)
    }

    fun refundOrder(orderId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        if (order.paymentStatus != PaymentStatus.PAID) {
            throw RefundNotAllowedException(orderId)
        }

        val refundResult = paymentService.refundOrder(orderId)

        if (refundResult.success) {
            order.paymentStatus = PaymentStatus.REFUNDED
            order.updatedAt = Instant.now()
        }

        return orderRepository.save(order)
    }

    private fun findOrderAndValidateRestaurant(restaurantId: UUID, orderId: UUID): DeliveryOrder {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw OrderNotFoundException(orderId)

        if (order.restaurant.id != restaurantId) {
            throw UnauthorizedRestaurantAccessException(restaurantId)
        }

        return order
    }

    private fun requireSubmittedStatus(order: DeliveryOrder) {
        if (order.status != OrderStatus.SUBMITTED) {
            throw OrderAlreadyFinalizedException(order.status)
        }
    }

    private fun requireNotTerminal(order: DeliveryOrder) {
        if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED) {
            throw OrderAlreadyDeliveredException(order.id)
        }
    }
}
