package com.example.feastly.restaurant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "restaurants")
data class Restaurant(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val address: String,
    @Column(nullable = false) val cuisine: String
)

