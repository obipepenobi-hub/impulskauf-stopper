package com.liam.kaptalismusaufhalter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WishStatus { PENDING, BOUGHT, SKIPPED }

@Entity
data class Wish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val createdAt: Long,
    val unlockAt: Long,
    val status: WishStatus,
    val decidedAt: Long? = null
)
