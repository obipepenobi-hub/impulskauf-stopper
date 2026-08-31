package com.liam.kaptalismusaufhalter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WishDao {
    @Insert
    suspend fun insert(wish: Wish): Long

    @Update
    suspend fun update(wish: Wish)

    @Query("SELECT * FROM Wish WHERE id = :id")
    suspend fun getById(id: Long): Wish?

    @Query("SELECT * FROM Wish WHERE id = :id")
    fun observeById(id: Long): Flow<Wish?>

    @Query("SELECT * FROM Wish WHERE status = :status ORDER BY unlockAt ASC")
    fun observeByStatus(status: WishStatus): Flow<List<Wish>>

    @Query("SELECT * FROM Wish WHERE status = 'PENDING' ORDER BY unlockAt ASC LIMIT :limit")
    fun observePendingPreview(limit: Int): Flow<List<Wish>>
}
