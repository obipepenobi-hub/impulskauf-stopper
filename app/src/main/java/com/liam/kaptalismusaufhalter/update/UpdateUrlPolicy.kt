package com.liam.kaptalismusaufhalter.update

import java.net.URI

/**
 * Where an update APK may come from. The update info is parsed out of a network response, so the
 * download URL is treated as untrusted input: it must point at *this app's own* GitHub release
 * downloads over HTTPS - nothing else is ever fetched and handed to the package installer.
 */
object UpdateUrlPolicy {

    fun isAllowedDownloadUrl(url: String, owner: String, repo: String): Boolean {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return false
        }
        val path = uri.rawPath ?: return false
        return uri.scheme == "https" &&
            uri.host == "github.com" &&
            uri.port == -1 &&
            uri.userInfo == null &&
            path.startsWith("/$owner/$repo/releases/download/", ignoreCase = true) &&
            !path.contains("..")
    }

    /** GitHub redirects release downloads to its CDN; after redirects the host must still be GitHub's. */
    fun isAllowedFinalHost(host: String?): Boolean =
        host != null && (host == "github.com" || host.endsWith(".githubusercontent.com"))

    /**
     * An update may change the signing key only through Android's key-rotation lineage. So the
     * currently installed signer has to appear in the downloaded APK's signer set or its lineage
     * (history) - otherwise it is a different publisher's app wearing our name.
     */
    fun <T> lineageAcceptsInstalled(installed: Set<T>, downloadedCurrent: Set<T>, downloadedHistory: Set<T>): Boolean =
        installed.isNotEmpty() && installed.all { it in downloadedCurrent || it in downloadedHistory }
}
