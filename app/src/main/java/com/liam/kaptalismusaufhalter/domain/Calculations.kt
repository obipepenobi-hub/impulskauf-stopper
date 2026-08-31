package com.liam.kaptalismusaufhalter.domain

import com.liam.kaptalismusaufhalter.data.WaitTier

fun calcWorkHours(price: Double, hourlyWage: Double): Double =
    if (hourlyWage > 0) price / hourlyWage else 0.0

fun calcWaitHours(price: Double, config: List<WaitTier>, factor: Double = 1.0): Int {
    val tier = config.firstOrNull { it.maxPrice == null || price <= it.maxPrice }
    val base = tier?.waitHours ?: 336
    return Math.round(base * factor).toInt()
}

data class PiggyStage(val threshold: Double, val name: String)

val PIGGY_STAGES = listOf(
    PiggyStage(0.0, "Ferkel"),
    PiggyStage(100.0, "Sparferkel"),
    PiggyStage(250.0, "Prachtsau"),
    PiggyStage(500.0, "Goldschwein"),
    PiggyStage(1000.0, "Zuchtlegende")
)

fun currentStage(total: Double): PiggyStage = PIGGY_STAGES.last { total >= it.threshold }
fun nextStage(total: Double): PiggyStage? = PIGGY_STAGES.firstOrNull { total < it.threshold }
