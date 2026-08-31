package com.liam.kaptalismusaufhalter.ui.screens.decision

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.PiggyBankEntry
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.data.Wish
import com.liam.kaptalismusaufhalter.data.WishStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DecisionViewModel(application: Application, private val wishId: Long) : AndroidViewModel(application) {
    private val database = getApplication<ImpulskaufApp>().database

    private val _wish = MutableStateFlow<Wish?>(null)
    val wish: StateFlow<Wish?> = _wish

    private val _decided = MutableStateFlow(false)
    val decided: StateFlow<Boolean> = _decided

    val piggyTotal: StateFlow<Double> = database.piggyBankDao()
        .observeTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val hourlyWage: StateFlow<Double> = database.settingsDao()
        .observe()
        .map { (it ?: Settings()).hourlyWage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings().hourlyWage)

    init {
        viewModelScope.launch {
            _wish.value = database.wishDao().getById(wishId)
        }
    }

    fun decide(bought: Boolean) {
        val current = _wish.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val status = if (bought) WishStatus.BOUGHT else WishStatus.SKIPPED
            database.wishDao().update(current.copy(status = status, decidedAt = now))
            if (!bought) {
                database.piggyBankDao().insert(
                    PiggyBankEntry(wishId = current.id, amount = current.price, timestamp = now)
                )
            }
            _decided.value = true
        }
    }

    companion object {
        fun factory(wishId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as Application
                DecisionViewModel(application, wishId)
            }
        }
    }
}
