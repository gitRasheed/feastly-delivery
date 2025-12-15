package com.feastly.drivertracking

import com.feastly.events.DriverLocationEvent
import com.feastly.events.KafkaTopics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.kafka.core.KafkaTemplate
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class DriverLocationAggregatorTest {

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private lateinit var aggregator: DriverLocationAggregator

    @BeforeEach
    fun setUp() {
        aggregator = DriverLocationAggregator(kafkaTemplate)
    }

    @Test
    fun `bufferUpdate stores event for later flush`() {
        val driverId = UUID.randomUUID()

        aggregator.bufferUpdate(driverId, 40.7128, -74.0060)

        verifyNoInteractions(kafkaTemplate)
    }

    @Test
    fun `flushBuffer does nothing when buffer is empty`() {
        aggregator.flushBuffer()

        verifyNoInteractions(kafkaTemplate)
    }

    @Test
    fun `flushBuffer sends all buffered events to Kafka`() {
        val driver1 = UUID.randomUUID()
        val driver2 = UUID.randomUUID()

        aggregator.bufferUpdate(driver1, 40.7128, -74.0060)
        aggregator.bufferUpdate(driver2, 34.0522, -118.2437)

        aggregator.flushBuffer()

        verify(kafkaTemplate, times(2)).send(eq(KafkaTopics.DRIVER_LOCATIONS), any<String>(), any())
    }

    @Test
    fun `flushBuffer sends event with correct driver ID as key`() {
        val driverId = UUID.randomUUID()
        aggregator.bufferUpdate(driverId, 40.7128, -74.0060)

        aggregator.flushBuffer()

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(kafkaTemplate).send(
            eq(KafkaTopics.DRIVER_LOCATIONS),
            eq(driverId.toString()),
            eventCaptor.capture()
        )

        val event = eventCaptor.value as DriverLocationEvent
        assertEquals(driverId, event.driverId)
        assertEquals(40.7128, event.latitude, 0.0001)
        assertEquals(-74.0060, event.longitude, 0.0001)
    }

    @Test
    fun `flushBuffer clears buffer after sending`() {
        val driverId = UUID.randomUUID()
        aggregator.bufferUpdate(driverId, 40.7128, -74.0060)

        aggregator.flushBuffer()
        aggregator.flushBuffer()

        verify(kafkaTemplate, times(1)).send(any<String>(), any<String>(), any())
    }

    @Test
    fun `bufferUpdate overwrites previous location for same driver`() {
        val driverId = UUID.randomUUID()

        aggregator.bufferUpdate(driverId, 40.7128, -74.0060)
        aggregator.bufferUpdate(driverId, 41.0000, -75.0000)

        aggregator.flushBuffer()

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(kafkaTemplate, times(1)).send(any<String>(), any<String>(), eventCaptor.capture())

        val event = eventCaptor.value as DriverLocationEvent
        assertEquals(41.0, event.latitude, 0.0001)
        assertEquals(-75.0, event.longitude, 0.0001)
    }
}
