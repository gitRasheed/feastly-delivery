package com.example.feastly.client

import java.util.UUID

interface UserClient {
    fun existsById(userId: UUID): Boolean
}
