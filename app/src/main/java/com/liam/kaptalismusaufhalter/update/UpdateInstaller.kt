package com.liam.kaptalismusaufhalter.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads the APK asset from a GitHub release via the system DownloadManager
 * (shows native progress in the notification shade) and, once complete, launches
 * the package installer. Requires the user to have granted "install unknown apps"
 * for this app — Android prompts for that automatically on the install screen.
 */
object UpdateInstaller {

    fun download(context: Context, info: UpdateInfo) {
        val fileName = "impulskauf-update-${info.versionName}.apk"
        val destination = File(context.getExternalFilesDir("downloads"), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(info.apkDownloadUrl))
            .setTitle("Impulskauf-Stopper Update ${info.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == downloadId) {
                    ctx.unregisterReceiver(this)
                    promptInstall(ctx, destination)
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun promptInstall(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
