package com.feastly.drivertracking

import com.feastly.events.DriverLocationEvent
import com.feastly.events.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DriverLocationAggregator(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(DriverLocationAggregator::class.java)
    private val buffer = ConcurrentHashMap<UUID, DriverLocationEvent>()

    fun bufferUpdate(driverId: UUID, latitude: Double, longitude: Double) {
        val event = DriverLocationEvent(
            driverId = driverId,
            latitude = latitude,
            longitude = longitude
        )
        buffer[driverId] = event
    }

    @Scheduled(fixedRate = 5000)
    fun flushBuffer() {
        if (buffer.isEmpty()) return

        val snapshot = buffer.toMap()
        buffer.clear()

        snapshot.forEach { (driverId, event) ->
            kafkaTemplate.send(KafkaTopics.DRIVER_LOCATIONS, driverId.toString(), event)
        }

        logger.debug("Flushed {} driver location events to Kafka", snapshot.size)
    }
}
