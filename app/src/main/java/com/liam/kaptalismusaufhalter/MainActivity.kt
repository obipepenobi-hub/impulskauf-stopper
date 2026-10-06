package com.liam.kaptalismusaufhalter

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.liam.kaptalismusaufhalter.security.IntentGuard
import com.liam.kaptalismusaufhalter.security.SecurityGuard
import com.liam.kaptalismusaufhalter.security.SecurityPrefs
import com.liam.kaptalismusaufhalter.ui.components.BottomNavBar
import com.liam.kaptalismusaufhalter.ui.navigation.AppNavGraph
import com.liam.kaptalismusaufhalter.ui.navigation.Destination
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ImpulskaufTheme
import com.liam.kaptalismusaufhalter.update.UpdateAvailableDialog
import com.liam.kaptalismusaufhalter.update.UpdateChecker
import com.liam.kaptalismusaufhalter.update.UpdateInfo
import com.liam.kaptalismusaufhalter.update.UpdateInstaller
import com.liam.kaptalismusaufhalter.work.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    private lateinit var securityPrefs: SecurityPrefs

    // Route requested by one of this app's own notifications; consumed once by the nav host.
    private var pendingRoute by mutableStateOf<String?>(null)

    // Held in a field: SharedPreferences only keeps weak references to its listeners.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyScreenProtection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityPrefs = SecurityPrefs(this)
        hardenWindow()
        requestNotificationPermissionIfNeeded()
        // After rotation / process recreation the nav back stack is restored by itself - handling
        // the original launch intent again would push the same screen a second time.
        if (savedInstanceState == null) handleLaunchIntent(intent)

        setContent {
            ImpulskaufTheme {
                val navController = rememberNavController()
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                var downloadProgress by remember { mutableStateOf<Float?>(null) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    updateInfo = UpdateChecker.checkForUpdate(
                        BuildConfig.UPDATE_REPO_OWNER,
                        BuildConfig.UPDATE_REPO_NAME,
                        BuildConfig.VERSION_NAME
                    )
                }
                val route = pendingRoute
                LaunchedEffect(route) {
                    route?.let {
                        navController.navigate(it)
                        pendingRoute = null
                    }
                }

                updateInfo?.let { info ->
                    UpdateAvailableDialog(
                        info = info,
                        downloadProgress = downloadProgress,
                        onDownload = {
                            downloadProgress = 0f
                            coroutineScope.launch {
                                UpdateInstaller.download(this@MainActivity, info) { progress ->
                                    downloadProgress = progress
                                }.onSuccess { apkFile ->
                                    UpdateInstaller.promptInstall(this@MainActivity, apkFile)
                                    downloadProgress = null
                                    updateInfo = null
                                }.onFailure {
                                    downloadProgress = null
                                }
                            }
                        },
                        onDismiss = { updateInfo = null }
                    )
                }

                // The design never shows a persistent app title bar - each screen renders its
                // own header. Start builds its own settings icon inline, so only show this
                // minimal floating icon on the other screens.
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val showSettingsIcon = currentRoute != null && currentRoute != Destination.Start.route

                // On every other screen, back just pops the nav back stack as usual (handled by
                // NavHost itself). Only on the Start screen - where back would otherwise exit
                // immediately - require a second press within 2s, so a stray back tap doesn't
                // accidentally close the app.
                val context = LocalContext.current
                var lastBackPressAt by remember { mutableStateOf(0L) }
                BackHandler(enabled = currentRoute == Destination.Start.route) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressAt < 2000L) {
                        (context as ComponentActivity).finish()
                    } else {
                        lastBackPressAt = now
                        Toast.makeText(context, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    topBar = {
                        if (showSettingsIcon) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                                    Icon(Icons.Filled.Tune, contentDescription = getString(R.string.settings_title))
                                }
                            }
                        }
                    },
                    bottomBar = { BottomNavBar(navController) },
                    containerColor = ColorBg
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        AppNavGraph(navController, onSettingsClick = { navController.navigate(Destination.Settings.route) })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        securityPrefs.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        applyScreenProtection()
        // Quick check every time the app comes to the front - catches anything the background
        // scan hasn't seen yet.
        lifecycleScope.launch { SecurityGuard.scan(this@MainActivity) }
    }

    override fun onStop() {
        super.onStop()
        securityPrefs.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    /**
     * This activity has to be exported (launcher), so any app can send it an intent. Extras that
     * drive navigation are only honored when they carry the secret from this app's own
     * notification PendingIntents - anything else is ignored (and reported if it clearly came
     * from another app).
     */
    private fun handleLaunchIntent(intent: Intent) {
        val wishId = intent.getLongExtra(NotificationHelper.EXTRA_WISH_ID, -1L).takeIf { it > 0 }
        val openSecurity = intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_SECURITY, false)
        if (wishId == null && !openSecurity) return

        if (!IntentGuard.isTrusted(this, intent)) {
            val caller = referrer?.authority
            // Notifications posted by an older version of the app carry no token - those come from
            // ourselves, so they are ignored quietly rather than reported as an attack.
            if (caller != null && caller != packageName) SecurityGuard.reportUnexpectedLaunch(this, caller)
            return
        }
        pendingRoute = when {
            openSecurity -> Destination.Security.route
            else -> Destination.Decision.route(wishId!!)
        }
    }

    /**
     * Locks the app window against the usual ways another app can attack it: tapjacking
     * overlays, screenshots/screen recording and accessibility services reading its content.
     */
    private fun hardenWindow() {
        window.decorView.filterTouchesWhenObscured = true
        // Each extra measure is best effort and isolated: a missing permission or an OEM quirk in
        // one of them must never be able to crash the app at launch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { window.setHideOverlayWindows(true) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { window.decorView.setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES) }
        }
        applyScreenProtection()
    }

    private fun applyScreenProtection() {
        val protect = securityPrefs.screenProtection
        if (protect) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { setRecentsScreenshotEnabled(!protect) }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
