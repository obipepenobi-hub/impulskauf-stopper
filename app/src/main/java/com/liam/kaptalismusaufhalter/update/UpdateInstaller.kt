package com.liam.kaptalismusaufhalter.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads the APK asset from a GitHub release via the system DownloadManager
 * (shows native progress in the notification shade) and, once complete, launches
 * the package installer. Requires the user to have granted "install unknown apps"
 * for this app — Android prompts for that automatically on the install screen.
 *
 * Completion is picked up two ways, because a dynamically-registered BroadcastReceiver
 * dies with the app process (the download can easily outlive the app if it's backgrounded
 * or killed by the OS while waiting): a manifest-declared [UpdateDownloadReceiver] for
 * ACTION_DOWNLOAD_COMPLETE, plus a [checkPendingDownload] fallback that re-checks
 * DownloadManager directly on the next app start in case the broadcast never arrived at all
 * (some OEM battery-optimization skins drop it entirely regardless of how it's registered).
 */
object UpdateInstaller {
    private const val PREFS = "update_installer"
    private const val KEY_DOWNLOAD_ID = "pending_download_id"
    private const val KEY_DOWNLOAD_PATH = "pending_download_path"

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

        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .putString(KEY_DOWNLOAD_PATH, destination.absolutePath)
            .apply()
    }

    /** Call on app start: if a previously started download already finished while we were
     * dead or backgrounded (and its completion broadcast never reached us), pick it up now. */
    fun checkPendingDownload(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1)
        val path = prefs.getString(KEY_DOWNLOAD_PATH, null)
        if (downloadId == -1L || path == null) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusIndex >= 0) it.getInt(statusIndex) else -1
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        clearPending(context)
                        promptInstall(context, File(path))
                    }
                    DownloadManager.STATUS_FAILED -> clearPending(context)
                    // still running/pending: leave it, the receiver or a later check will catch it
                }
            } else {
                // download record is gone (cleared from the system download manager)
                clearPending(context)
            }
        }
    }

    internal fun handleDownloadComplete(context: Context, completedId: Long) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1)
        val path = prefs.getString(KEY_DOWNLOAD_PATH, null)
        if (downloadId == -1L || path == null || completedId != downloadId) return

        clearPending(context)
        promptInstall(context, File(path))
    }

    private fun clearPending(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DOWNLOAD_ID)
            .remove(KEY_DOWNLOAD_PATH)
            .apply()
    }

    private fun promptInstall(context: Context, apkFile: File) {
        if (!apkFile.exists()) return
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
