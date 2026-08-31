package com.liam.kaptalismusaufhalter.ui.screens.piggybank

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.PiggyEntryWithWish
import com.liam.kaptalismusaufhalter.domain.PiggyStage
import com.liam.kaptalismusaufhalter.domain.currentStage
import com.liam.kaptalismusaufhalter.domain.nextStage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PiggyBankUiState(
    val total: Double = 0.0,
    val stage: PiggyStage = currentStage(0.0),
    val nextStage: PiggyStage? = nextStage(0.0),
    val history: List<PiggyEntryWithWish> = emptyList()
)

class PiggyBankViewModel(application: Application) : AndroidViewModel(application) {
    private val database = getApplication<ImpulskaufApp>().database

    val uiState = combine(
        database.piggyBankDao().observeTotal(),
        database.piggyBankDao().observeHistory()
    ) { total, history ->
        PiggyBankUiState(
            total = total,
            stage = currentStage(total),
            nextStage = nextStage(total),
            history = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PiggyBankUiState())
}
