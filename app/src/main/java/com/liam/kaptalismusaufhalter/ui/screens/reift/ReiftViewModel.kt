package com.liam.kaptalismusaufhalter.ui.screens.reift

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.data.Wish
import com.liam.kaptalismusaufhalter.data.WishStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReiftViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<ImpulskaufApp>()
    private val database = app.database

    val pendingWishes: StateFlow<List<Wish>> = database.wishDao()
        .observeByStatus(WishStatus.PENDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyWage: StateFlow<Double> = database.settingsDao()
        .observe()
        .map { (it ?: Settings()).hourlyWage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings().hourlyWage)

    fun quickAdd(name: String, price: Double) {
        viewModelScope.launch {
            app.wishRepository.createWish(name, price)
        }
    }
}
