package com.liam.kaptalismusaufhalter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.liam.kaptalismusaufhalter.ui.components.BottomNavBar
import com.liam.kaptalismusaufhalter.ui.navigation.AppNavGraph
import com.liam.kaptalismusaufhalter.ui.navigation.Destination
import com.liam.kaptalismusaufhalter.ui.theme.ImpulskaufTheme
import com.liam.kaptalismusaufhalter.update.UpdateAvailableDialog
import com.liam.kaptalismusaufhalter.update.UpdateChecker
import com.liam.kaptalismusaufhalter.update.UpdateInfo
import com.liam.kaptalismusaufhalter.update.UpdateInstaller
import com.liam.kaptalismusaufhalter.work.NotificationHelper

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val initialWishId = intent.getLongExtra(NotificationHelper.EXTRA_WISH_ID, -1L).takeIf { it > 0 }

        setContent {
            ImpulskaufTheme {
                val navController = rememberNavController()
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

                LaunchedEffect(Unit) {
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

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(getString(R.string.app_name)) },
                            actions = {
                                IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                                    Icon(Icons.Filled.Tune, contentDescription = getString(R.string.settings_title))
                                }
                            }
                        )
                    },
                    bottomBar = { BottomNavBar(navController) }
                ) { padding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
                        AppNavGraph(navController)
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
