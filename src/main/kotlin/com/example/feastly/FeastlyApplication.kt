package com.example.feastly

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeastlyApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
	runApplication<FeastlyApplication>(*args)
}
