package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.venom.data.local.entity.UserIdentityEntity

@Dao
interface UserIdentityDao {
    
    @Query("SELECT * FROM userIdentity WHERE id = 1")
    suspend fun getIdentity(): UserIdentityEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(identity: UserIdentityEntity)
    
    @Query("UPDATE userIdentity SET lastActiveAt = :timestamp WHERE id = 1")
    suspend fun updateLastActive(timestamp: Long)

    @Query("DELETE FROM userIdentity WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
