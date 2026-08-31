package com.liam.kaptalismusaufhalter.ui.screens.piggybank

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.PiggyEntryWithWish
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PiggyBankUiState(
    val total: Double = 0.0,
    val workHours: Double = 0.0,
    val hourlyWage: Double = 0.0,
    val history: List<PiggyEntryWithWish> = emptyList()
)

class PiggyBankViewModel(application: Application) : AndroidViewModel(application) {
    private val database = getApplication<ImpulskaufApp>().database

    val uiState = combine(
        database.piggyBankDao().observeTotal(),
        database.piggyBankDao().observeHistory(),
        database.settingsDao().observe()
    ) { total, history, settings ->
        val wage = (settings ?: Settings()).hourlyWage
        PiggyBankUiState(
            total = total,
            workHours = calcWorkHours(total, wage),
            hourlyWage = wage,
            history = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PiggyBankUiState())
}
