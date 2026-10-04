package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userIdentity")
data class UserIdentityEntity(
    @PrimaryKey val id: Int = 1,
    val userId: String,
    val deviceId: String,
    val isAnonymous: Boolean,
    val createdAt: Long,
    val lastActiveAt: Long
)
