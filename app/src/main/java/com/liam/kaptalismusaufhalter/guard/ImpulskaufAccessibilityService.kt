package com.liam.kaptalismusaufhalter.guard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ImpulskaufAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var overlay: OverlayController

    private var lastEventAt = 0L
    private val cooldownUntil = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        if (overlay.isShowing()) return

        val now = System.currentTimeMillis()
        if (now - lastEventAt < DEBOUNCE_MS) return
        lastEventAt = now

        cooldownUntil[packageName]?.let { until ->
            if (now < until) return
        }

        // Captured here, synchronously on the event callback, so it can't drift to a
        // different app's content if the user switches apps before the coroutine runs.
        val root = rootInActiveWindow ?: return

        scope.launch {
            val app = applicationContext as ImpulskaufApp
            val excluded = app.database.excludedAppDao().getAll()
            if (packageName in excluded) return@launch

            val signal = PurchaseDetector.detect(root) ?: return@launch

            withMain {
                showOverlay(packageName, signal.price)
            }
        }
    }

    private fun showOverlay(packageName: String, detectedPrice: Double?) {
        if (!Settings.canDrawOverlays(this)) return
        if (overlay.isShowing()) return

        val appLabel = resolveAppLabel(packageName)

        overlay.show { dismiss ->
            ImpulsPopupOverlay(
                detectedPrice = detectedPrice,
                sourceAppLabel = appLabel,
                onRipen = { name, price ->
                    scope.launch {
                        (applicationContext as ImpulskaufApp).wishRepository.createWish(name, price)
                        withMain {
                            cooldownUntil[packageName] = System.currentTimeMillis() + COOLDOWN_MS
                            dismiss()
                        }
                    }
                },
                onBuyAnyway = {
                    cooldownUntil[packageName] = System.currentTimeMillis() + COOLDOWN_MS
                    dismiss()
                },
                onClose = {
                    // Treated like "trotzdem kaufen" for cooldown purposes - a false-positive
                    // trigger closed via the X shouldn't immediately pop up again on the same screen.
                    cooldownUntil[packageName] = System.currentTimeMillis() + COOLDOWN_MS
                    dismiss()
                }
            )
        }
    }

    private fun resolveAppLabel(packageName: String): String =
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            packageName
        }

    private suspend fun withMain(block: () -> Unit) {
        kotlinx.coroutines.withContext(Dispatchers.Main) { block() }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (::overlay.isInitialized) overlay.hide()
        scope.cancel()
    }

    companion object {
        private const val DEBOUNCE_MS = 500L
        private const val COOLDOWN_MS = 5 * 60 * 1000L

        fun isEnabled(context: Context): Boolean {
            val enabledServices = ContextCompat.getSystemService(context, android.view.accessibility.AccessibilityManager::class.java)
                ?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC)
                ?: return false
            return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        }
    }
}
