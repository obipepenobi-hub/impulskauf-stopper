package com.liam.kaptalismusaufhalter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PiggyBankEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wishId: Long,
    val amount: Double,
    val timestamp: Long
)
