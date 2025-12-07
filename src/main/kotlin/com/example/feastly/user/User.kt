package com.example.feastly.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(unique = true)
    val email: String,
    val password: String
)
