package com.liam.kaptalismusaufhalter.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromWishStatus(status: WishStatus): String = status.name

    @TypeConverter
    fun toWishStatus(value: String): WishStatus = WishStatus.valueOf(value)
}
