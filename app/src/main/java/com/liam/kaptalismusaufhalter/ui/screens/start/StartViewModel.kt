package com.liam.kaptalismusaufhalter.ui.screens.start

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.data.Wish
import com.liam.kaptalismusaufhalter.domain.PiggyStage
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import com.liam.kaptalismusaufhalter.domain.currentStage
import com.liam.kaptalismusaufhalter.domain.nextStage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StartUiState(
    val total: Double = 0.0,
    val stage: PiggyStage = currentStage(0.0),
    val nextStage: PiggyStage? = nextStage(0.0),
    val workHours: Double = 0.0,
    val ripeningPreview: List<Wish> = emptyList()
)

class StartViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<ImpulskaufApp>()
    private val database = app.database

    val uiState = combine(
        database.piggyBankDao().observeTotal(),
        database.wishDao().observePendingPreview(3),
        database.settingsDao().observe()
    ) { total, preview, settings ->
        StartUiState(
            total = total,
            stage = currentStage(total),
            nextStage = nextStage(total),
            workHours = calcWorkHours(total, (settings ?: Settings()).hourlyWage),
            ripeningPreview = preview
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StartUiState())
}
