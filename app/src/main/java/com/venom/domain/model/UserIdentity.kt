package com.venom.domain.model

data class UserIdentity(
    val userId: String,
    val deviceId: String,
    val isAnonymous: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)
