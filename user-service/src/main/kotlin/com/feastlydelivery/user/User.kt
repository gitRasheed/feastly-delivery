package com.feastlydelivery.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val name: String,
    @Column(unique = true, nullable = false)
    val email: String
)
