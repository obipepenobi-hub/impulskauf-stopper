package com.liam.kaptalismusaufhalter.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.data.Strictness
import com.liam.kaptalismusaufhalter.domain.PiggyStage
import com.liam.kaptalismusaufhalter.domain.currentStage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: Settings = Settings(),
    val currentStage: PiggyStage = currentStage(0.0)
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = getApplication<ImpulskaufApp>().database

    val uiState: StateFlow<SettingsUiState> = combine(
        database.settingsDao().observe(),
        database.piggyBankDao().observeTotal()
    ) { settings, total ->
        SettingsUiState(settings = settings ?: Settings(), currentStage = currentStage(total))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateHourlyWage(wage: Double) {
        viewModelScope.launch {
            val current = database.settingsDao().get() ?: Settings()
            database.settingsDao().upsert(current.copy(hourlyWage = wage))
        }
    }

    fun updateStrictness(strictness: Strictness) {
        viewModelScope.launch {
            val current = database.settingsDao().get() ?: Settings()
            database.settingsDao().upsert(current.copy(strictness = strictness.name))
        }
    }
}
