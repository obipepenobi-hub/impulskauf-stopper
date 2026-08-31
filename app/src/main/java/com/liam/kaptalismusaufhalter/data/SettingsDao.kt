package com.liam.kaptalismusaufhalter.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM Settings WHERE id = 0")
    fun observe(): Flow<Settings?>

    @Query("SELECT * FROM Settings WHERE id = 0")
    suspend fun get(): Settings?

    @Upsert
    suspend fun upsert(settings: Settings)
}
