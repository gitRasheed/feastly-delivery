package com.feastly.dispatch

import com.feastly.events.OrderAcceptedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DispatchEventListenerTest {

    @Mock
    private lateinit var dispatchService: DispatchService

    @InjectMocks
    private lateinit var listener: DispatchEventListener

    @Test
    fun `handleOrderAccepted calls DispatchService startDispatch with correct orderId`() {
        val orderId = UUID.randomUUID()
        val restaurantId = UUID.randomUUID()

        val event = OrderAcceptedEvent(
            orderId = orderId,
            restaurantId = restaurantId,
            timestamp = Instant.now()
        )

        listener.handleOrderAccepted(event)

        verify(dispatchService).startDispatch(orderId)
    }
}
