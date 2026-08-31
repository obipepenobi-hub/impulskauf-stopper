package com.liam.kaptalismusaufhalter.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Manifest-declared so it survives the app process being killed while the download runs -
 * a dynamically registered receiver dies with the process, which is why updates could
 * silently stall after tapping "Herunterladen" (see UpdateInstaller for the full story).
 */
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (completedId == -1L) return
        UpdateInstaller.handleDownloadComplete(context, completedId)
    }
}
