package com.liam.kaptalismusaufhalter.ui.screens.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.kaptalismusaufhalter.data.AppDatabase
import com.liam.kaptalismusaufhalter.security.ComponentKind
import com.liam.kaptalismusaufhalter.security.RiskAssessment
import com.liam.kaptalismusaufhalter.security.RiskAssessor
import com.liam.kaptalismusaufhalter.security.SecurityCandidate
import com.liam.kaptalismusaufhalter.security.SecurityEvent
import com.liam.kaptalismusaufhalter.security.SecurityGuard
import com.liam.kaptalismusaufhalter.security.SecurityLog
import com.liam.kaptalismusaufhalter.security.SecurityPrefs
import com.liam.kaptalismusaufhalter.security.WishImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ComponentRow(
    val candidate: SecurityCandidate,
    val assessment: RiskAssessment,
    val trusted: Boolean
)

data class SecurityUiState(
    val screenProtection: Boolean = true,
    val capturePhotos: Boolean = true,
    val guardEnabled: Boolean = true,
    val events: List<SecurityEvent> = emptyList(),
    val components: List<ComponentRow> = emptyList(),
    val failedKinds: Set<ComponentKind> = emptySet(),
    val scanning: Boolean = false,
    val lastScanAt: Long = 0L,
    val photosDeleted: Boolean = false
)

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Application = application
    private val prefs = SecurityPrefs(app)
    private val log = SecurityLog(app)

    private var lastCandidates: List<SecurityCandidate> = emptyList()
    private var lastFailedKinds: Set<ComponentKind> = emptySet()

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            screenProtection = prefs.screenProtection,
            capturePhotos = prefs.capturePhotos,
            guardEnabled = prefs.guardEnabled
        )
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(scanning = true) }
            val summary = SecurityGuard.scan(app)
            if (summary != null) {
                lastCandidates = summary.candidates
                lastFailedKinds = summary.failedKinds
            }
            reload(scanning = false)
            // Opening this screen counts as having seen everything logged so far.
            prefs.lastSeenEventAt = System.currentTimeMillis()
        }
    }

    fun setScreenProtection(enabled: Boolean) {
        prefs.screenProtection = enabled
        _uiState.update { it.copy(screenProtection = enabled) }
    }

    fun setCapturePhotos(enabled: Boolean) {
        prefs.capturePhotos = enabled
        _uiState.update { it.copy(capturePhotos = enabled) }
    }

    fun setGuardEnabled(enabled: Boolean) {
        prefs.guardEnabled = enabled
        _uiState.update { it.copy(guardEnabled = enabled) }
    }

    fun toggleTrust(key: String) {
        val currentlyTrusted = key in prefs.loadGuardState().trustedKeys
        prefs.setTrusted(key, !currentlyTrusted)
        viewModelScope.launch { reload(scanning = false) }
    }

    fun clearLog() {
        log.clear()
        viewModelScope.launch { reload(scanning = false) }
    }

    fun deleteAllPhotos() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                WishImageStore.deleteAll(app)
                AppDatabase.getInstance(app).wishDao().clearImagePaths()
            }
            _uiState.update { it.copy(photosDeleted = true) }
        }
    }

    private suspend fun reload(scanning: Boolean) {
        val state = withContext(Dispatchers.IO) {
            val trusted = prefs.loadGuardState().trustedKeys
            val rows = lastCandidates
                .filter { it.packageName != app.packageName }
                .map { ComponentRow(it, RiskAssessor.assess(it), it.key in trusted) }
                .sortedWith(
                    compareByDescending<ComponentRow> { it.assessment.level.ordinal }
                        .thenBy { it.candidate.label.lowercase() }
                )
            SecurityUiState(
                screenProtection = prefs.screenProtection,
                capturePhotos = prefs.capturePhotos,
                guardEnabled = prefs.guardEnabled,
                events = log.all(),
                components = rows,
                failedKinds = lastFailedKinds,
                scanning = scanning,
                lastScanAt = prefs.lastScanAt,
                photosDeleted = _uiState.value.photosDeleted
            )
        }
        _uiState.value = state
    }
}
