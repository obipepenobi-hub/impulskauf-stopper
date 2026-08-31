package com.liam.kaptalismusaufhalter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A package the Impulskauf-Stopper accessibility service should never trigger on. */
@Entity
data class ExcludedApp(
    @PrimaryKey val packageName: String
)
