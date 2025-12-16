package com.feastly.drivertracking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DriverTrackingServiceApplication

fun main(args: Array<String>) {
    runApplication<DriverTrackingServiceApplication>(*args)
}
