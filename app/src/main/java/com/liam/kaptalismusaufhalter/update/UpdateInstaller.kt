package com.liam.kaptalismusaufhalter.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.FileProvider
import com.liam.kaptalismusaufhalter.BuildConfig
import com.liam.kaptalismusaufhalter.security.SecurityGuard
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
 *
 * Hardening, so a hijacked response or a malicious app can't turn "update" into "install malware":
 * - the URL must be this app's own GitHub release download over HTTPS ([UpdateUrlPolicy]) and
 *   must still be on GitHub's hosts after redirects,
 * - the APK is stored in the app's private cache - other apps can't swap it between download and
 *   install, which they could on shared external storage,
 * - the download is size-capped,
 * - the APK's signing certificate must match (or be a legitimate key-rotation successor of) the
 *   installed app's before the installer is ever launched.
 * Any violation is logged and raised as a security warning.
 */
object UpdateInstaller {

    private const val MAX_APK_BYTES = 150L * 1024 * 1024

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        try {
            if (!UpdateUrlPolicy.isAllowedDownloadUrl(info.apkDownloadUrl, BuildConfig.UPDATE_REPO_OWNER, BuildConfig.UPDATE_REPO_NAME)) {
                return@withContext rejected(app, "Die Download-Adresse des Updates gehört nicht zum offiziellen Release dieser App.")
            }

            val dir = File(app.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val destination = File(dir, "impulskauf-update-${info.versionName.filter { it.isLetterOrDigit() || it == '.' }}.apk")

            val connection = (URL(info.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
                }
                if (!UpdateUrlPolicy.isAllowedFinalHost(connection.url.host)) {
                    return@withContext rejected(app, "Der Download wurde auf einen fremden Server umgeleitet (${connection.url.host}).")
                }

                val totalBytes = connection.contentLengthLong
                if (totalBytes > MAX_APK_BYTES) {
                    return@withContext rejected(app, "Die Update-Datei ist ungewöhnlich groß.")
                }

                connection.inputStream.use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var readBytes = 0L
                        var count: Int
                        while (input.read(buffer).also { count = it } != -1) {
                            readBytes += count
                            if (readBytes > MAX_APK_BYTES) {
                                destination.delete()
                                return@withContext rejected(app, "Die Update-Datei ist ungewöhnlich groß.")
                            }
                            output.write(buffer, 0, count)
                            if (totalBytes > 0) {
                                onProgress((readBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (!signatureAcceptable(app, destination)) {
                destination.delete()
                return@withContext rejected(app, "Die Signatur des Updates passt nicht zur installierten App.")
            }
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun rejected(context: Context, reason: String): Result<File> {
        SecurityGuard.reportUpdateRejected(context, reason)
        return Result.failure(SecurityException(reason))
    }

    @Suppress("DEPRECATION")
    private fun signatureAcceptable(context: Context, apk: File): Boolean = try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val flag = PackageManager.GET_SIGNING_CERTIFICATES
            val downloaded = pm.getPackageArchiveInfo(apk.path, flag)?.signingInfo
            val installed = pm.getPackageInfo(context.packageName, flag).signingInfo
            val current: Set<Signature> = downloaded?.apkContentsSigners?.toSet().orEmpty()
            val history: Set<Signature> =
                if (downloaded?.hasPastSigningCertificates() == true) downloaded.signingCertificateHistory.toSet() else current
            UpdateUrlPolicy.lineageAcceptsInstalled(installed?.apkContentsSigners?.toSet().orEmpty(), current, history)
        } else {
            val flag = PackageManager.GET_SIGNATURES
            val downloaded = pm.getPackageArchiveInfo(apk.path, flag)?.signatures?.toSet().orEmpty()
            val installed = pm.getPackageInfo(context.packageName, flag).signatures?.toSet().orEmpty()
            UpdateUrlPolicy.lineageAcceptsInstalled(installed, downloaded, downloaded)
        }
    } catch (e: Exception) {
        false
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
