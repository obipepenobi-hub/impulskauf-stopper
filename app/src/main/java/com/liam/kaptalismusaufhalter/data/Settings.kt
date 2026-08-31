package com.liam.kaptalismusaufhalter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class WaitTier(val maxPrice: Double?, val waitHours: Int)

val DEFAULT_WAIT_TIERS = listOf(
    WaitTier(20.0, 4),
    WaitTier(50.0, 24),
    WaitTier(150.0, 72),
    WaitTier(400.0, 168),
    WaitTier(null, 336)
)

fun List<WaitTier>.toJson(): String {
    val arr = JSONArray()
    for (tier in this) {
        val obj = JSONObject()
        if (tier.maxPrice != null) obj.put("maxPrice", tier.maxPrice) else obj.put("maxPrice", JSONObject.NULL)
        obj.put("waitHours", tier.waitHours)
        arr.put(obj)
    }
    return arr.toString()
}

fun String.toWaitTiers(): List<WaitTier> {
    return try {
        val arr = JSONArray(this)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val maxPrice = if (obj.isNull("maxPrice")) null else obj.getDouble("maxPrice")
            WaitTier(maxPrice, obj.getInt("waitHours"))
        }
    } catch (e: Exception) {
        DEFAULT_WAIT_TIERS
    }
}

@Entity
data class Settings(
    @PrimaryKey val id: Int = 0,
    val hourlyWage: Double = 15.0,
    val waitTimeConfig: String = DEFAULT_WAIT_TIERS.toJson()
)
