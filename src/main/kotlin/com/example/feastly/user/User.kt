package com.example.feastly.user

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "users")
data class User(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(unique = true)
    val email: String,
    val password: String
)
