package com.example.feastly

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class FeastlyApplication

fun main(args: Array<String>) {
	runApplication<FeastlyApplication>(*args)
}

