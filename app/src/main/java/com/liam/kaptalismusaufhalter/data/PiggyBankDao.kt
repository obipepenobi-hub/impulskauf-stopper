package com.liam.kaptalismusaufhalter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PiggyEntryWithWish(
    val id: Long,
    val amount: Double,
    val timestamp: Long,
    val wishName: String
)

@Dao
interface PiggyBankDao {
    @Insert
    suspend fun insert(entry: PiggyBankEntry): Long

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM PiggyBankEntry")
    fun observeTotal(): Flow<Double>

    @Query(
        """
        SELECT PiggyBankEntry.id AS id, amount, timestamp, Wish.name AS wishName
        FROM PiggyBankEntry
        JOIN Wish ON Wish.id = PiggyBankEntry.wishId
        ORDER BY timestamp DESC
        """
    )
    fun observeHistory(): Flow<List<PiggyEntryWithWish>>
}
