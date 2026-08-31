package com.liam.kaptalismusaufhalter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedAppDao {
    @Query("SELECT packageName FROM ExcludedApp")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT packageName FROM ExcludedApp")
    suspend fun getAll(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: ExcludedApp)

    @Delete
    suspend fun delete(app: ExcludedApp)
}
