package com.liam.kaptalismusaufhalter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val initialWishId = intent.getLongExtra(NotificationHelper.EXTRA_WISH_ID, -1L).takeIf { it > 0 }

        setContent {
            ImpulskaufTheme {
                val navController = rememberNavController()
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    // Picks up a download that finished while we were backgrounded/killed and
                    // whose completion broadcast we therefore missed.
                    UpdateInstaller.checkPendingDownload(this@MainActivity)
                    updateInfo = UpdateChecker.checkForUpdate(
                        BuildConfig.UPDATE_REPO_OWNER,
                        BuildConfig.UPDATE_REPO_NAME,
                        BuildConfig.VERSION_NAME
                    )
                }
                LaunchedEffect(initialWishId) {
                    initialWishId?.let { navController.navigate(Destination.Decision.route(it)) }
                }

                updateInfo?.let { info ->
                    UpdateAvailableDialog(
                        info = info,
                        onDownload = {
                            UpdateInstaller.download(this@MainActivity, info)
                            updateInfo = null
                        },
                        onDismiss = { updateInfo = null }
                    )
                }

                // The design never shows a persistent app title bar - each screen renders its
                // own header. Start builds its own settings icon inline, so only show this
                // minimal floating icon on the other screens.
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val showSettingsIcon = currentRoute != null && currentRoute != Destination.Start.route

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
