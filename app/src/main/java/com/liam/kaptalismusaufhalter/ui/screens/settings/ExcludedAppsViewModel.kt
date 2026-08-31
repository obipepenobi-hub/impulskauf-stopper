package com.liam.kaptalismusaufhalter.ui.screens.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.ExcludedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppInfo(val packageName: String, val label: String)

data class ExcludedAppsUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val excludedPackages: Set<String> = emptySet(),
    val loading: Boolean = true
)

class ExcludedAppsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = getApplication<ImpulskaufApp>().database
    private val appsFlow = MutableStateFlow<List<InstalledAppInfo>>(emptyList())

    val uiState: StateFlow<ExcludedAppsUiState> = combine(
        appsFlow,
        database.excludedAppDao().observeAll()
    ) { apps, excluded ->
        ExcludedAppsUiState(apps = apps, excludedPackages = excluded.toSet(), loading = apps.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExcludedAppsUiState())

    init {
        viewModelScope.launch {
            appsFlow.value = loadInstalledApps()
        }
    }

    private suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = getApplication<ImpulskaufApp>().packageManager
        val ownPackage = getApplication<ImpulskaufApp>().packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ownPackage }
            .map { InstalledAppInfo(it.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    fun setExcluded(packageName: String, excluded: Boolean) {
        viewModelScope.launch {
            if (excluded) {
                database.excludedAppDao().insert(ExcludedApp(packageName))
            } else {
                database.excludedAppDao().delete(ExcludedApp(packageName))
            }
        }
    }
}
