package de.daimpl.demo.client

data class UserDto(
    val userId: String,
    val username: String,
    val email: String,
    val registeredAt: Long,
    val preferences: Preferences,
    val tags: String
)

data class Preferences(val theme: String, val notifications: Boolean)