package com.liam.kaptalismusaufhalter.ui.screens.newwish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.data.toWaitTiers
import com.liam.kaptalismusaufhalter.domain.calcWaitHours
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewWishViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<ImpulskaufApp>()
    private val database = app.database

    val settings: StateFlow<Settings> = database.settingsDao()
        .observe()
        .map { it ?: Settings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun workHoursFor(price: Double): Double = calcWorkHours(price, settings.value.hourlyWage)

    fun waitHoursFor(price: Double): Int = calcWaitHours(price, settings.value.waitTimeConfig.toWaitTiers())

    fun save(name: String, price: Double, linkUrl: String?) {
        viewModelScope.launch {
            app.wishRepository.createWish(name, price, linkUrl?.takeIf { it.isNotBlank() })
            _saved.value = true
        }
    }

    fun resetSaved() {
        _saved.value = false
    }
}
