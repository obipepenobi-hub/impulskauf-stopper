package com.liam.kaptalismusaufhalter.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = getApplication<ImpulskaufApp>().database

    val settings: StateFlow<Settings> = database.settingsDao()
        .observe()
        .map { it ?: Settings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun updateHourlyWage(wage: Double) {
        viewModelScope.launch {
            val current = database.settingsDao().get() ?: Settings()
            database.settingsDao().upsert(current.copy(hourlyWage = wage))
        }
    }
}
