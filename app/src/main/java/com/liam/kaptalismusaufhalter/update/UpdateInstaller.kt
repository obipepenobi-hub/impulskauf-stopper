package com.liam.kaptalismusaufhalter.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the APK asset from a GitHub release directly inside the app process, streaming
 * progress back to the caller - no system DownloadManager, so no download notification and no
 * entry in the system Downloads app. Once complete, launches the package installer; Android
 * still shows its own "App installieren?" confirmation screen for that part, which can't be
 * skipped without root/system-app privileges.
 */
object UpdateInstaller {

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = "impulskauf-update-${info.versionName}.apk"
            val destination = File(context.getExternalFilesDir("downloads"), fileName)
            if (destination.exists()) destination.delete()

            val connection = (URL(info.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            connection.connect()

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLength
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var readBytes = 0L
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                        readBytes += count
                        if (totalBytes > 0) {
                            onProgress((readBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            connection.disconnect()
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstall(context: Context, apkFile: File) {
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
