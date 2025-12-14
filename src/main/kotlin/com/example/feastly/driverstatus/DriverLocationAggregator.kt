package com.example.feastly.driverstatus

import com.example.feastly.events.DriverLocationEvent
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

    companion object {
        private const val DRIVER_LOCATIONS_TOPIC = "driver.locations"
    }

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
            kafkaTemplate.send(DRIVER_LOCATIONS_TOPIC, driverId.toString(), event)
        }

        logger.debug("Flushed {} driver location events to Kafka", snapshot.size)
    }
}
