package com.feastlydelivery.restaurant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val name: String,
    val description: String? = null
)
