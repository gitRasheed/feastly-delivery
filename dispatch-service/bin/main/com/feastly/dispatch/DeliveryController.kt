package com.feastly.dispatch

import com.feastly.events.DeliveryCompletedEvent
import com.feastly.events.DriverDeliveryFailedEvent
import com.feastly.events.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/driver")
class DeliveryController(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(DeliveryController::class.java)

    @PostMapping("/{driverId}/completeDelivery")
    fun completeDelivery(
        @PathVariable driverId: UUID,
        @RequestParam orderId: UUID
    ): ResponseEntity<Void> {
        log.info("Driver $driverId completing delivery for order $orderId")

        val event = DeliveryCompletedEvent(
            orderId = orderId,
            driverId = driverId
        )
        kafkaTemplate.send(KafkaTopics.DISPATCH_EVENTS, orderId.toString(), event)
        log.info("Emitted DeliveryCompletedEvent for order $orderId")

        return ResponseEntity.ok().build()
    }

    @PostMapping("/{driverId}/failDelivery")
    fun failDelivery(
        @PathVariable driverId: UUID,
        @RequestParam orderId: UUID,
        @RequestParam(defaultValue = "Unknown reason") reason: String
    ): ResponseEntity<Void> {
        log.info("Driver $driverId failed delivery for order $orderId: $reason")

        val event = DriverDeliveryFailedEvent(
            orderId = orderId,
            driverId = driverId,
            reason = reason
        )
        kafkaTemplate.send(KafkaTopics.DISPATCH_EVENTS, orderId.toString(), event)
        log.info("Emitted DriverDeliveryFailedEvent for order $orderId")

        return ResponseEntity.ok().build()
    }
}
