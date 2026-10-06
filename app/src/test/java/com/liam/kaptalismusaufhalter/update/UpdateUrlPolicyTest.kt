package com.liam.kaptalismusaufhalter.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUrlPolicyTest {

    private val owner = "obipepenobi-hub"
    private val repo = "impulskauf-stopper"
    private val official = "https://github.com/$owner/$repo/releases/download/v1.2.0/impulskauf-stopper-1.2.0.apk"

    private fun allowed(url: String) = UpdateUrlPolicy.isAllowedDownloadUrl(url, owner, repo)

    @Test
    fun `the official release download is allowed`() {
        assertTrue(allowed(official))
    }

    @Test
    fun `owner and repo are matched case-insensitively like GitHub does`() {
        assertTrue(allowed("https://github.com/Obipepenobi-Hub/Impulskauf-Stopper/releases/download/v1/a.apk"))
    }

    @Test
    fun `plain http is rejected`() {
        assertFalse(allowed(official.replace("https://", "http://")))
    }

    @Test
    fun `other hosts are rejected including lookalikes and userinfo tricks`() {
        assertFalse(allowed("https://evil.example/$owner/$repo/releases/download/v1/a.apk"))
        assertFalse(allowed("https://github.com.evil.example/$owner/$repo/releases/download/v1/a.apk"))
        assertFalse(allowed("https://github.com@evil.example/$owner/$repo/releases/download/v1/a.apk"))
        assertFalse(allowed("https://evil.example@github.com/$owner/$repo/releases/download/v1/a.apk"))
    }

    @Test
    fun `other repositories and other paths are rejected`() {
        assertFalse(allowed("https://github.com/someone-else/$repo/releases/download/v1/a.apk"))
        assertFalse(allowed("https://github.com/$owner/other-app/releases/download/v1/a.apk"))
        assertFalse(allowed("https://github.com/$owner/$repo/archive/main.zip"))
        assertFalse(allowed("https://github.com/$owner/$repo/releases/download/../../../evil/a.apk"))
    }

    @Test
    fun `explicit ports and garbage are rejected`() {
        assertFalse(allowed("https://github.com:8443/$owner/$repo/releases/download/v1/a.apk"))
        assertFalse(allowed("not a url"))
        assertFalse(allowed(""))
    }

    @Test
    fun `redirects may only end on GitHub hosts`() {
        assertTrue(UpdateUrlPolicy.isAllowedFinalHost("github.com"))
        assertTrue(UpdateUrlPolicy.isAllowedFinalHost("objects.githubusercontent.com"))
        assertTrue(UpdateUrlPolicy.isAllowedFinalHost("release-assets.githubusercontent.com"))
        assertFalse(UpdateUrlPolicy.isAllowedFinalHost("githubusercontent.com.evil.example"))
        assertFalse(UpdateUrlPolicy.isAllowedFinalHost("evil.example"))
        assertFalse(UpdateUrlPolicy.isAllowedFinalHost(null))
    }

    @Test
    fun `same signer is accepted`() {
        assertTrue(UpdateUrlPolicy.lineageAcceptsInstalled(setOf("A"), setOf("A"), setOf("A")))
    }

    @Test
    fun `a different signer is rejected`() {
        assertFalse(UpdateUrlPolicy.lineageAcceptsInstalled(setOf("A"), setOf("B"), setOf("B")))
    }

    @Test
    fun `a legitimate key rotation is accepted - installed key is an ancestor in the new lineage`() {
        assertTrue(UpdateUrlPolicy.lineageAcceptsInstalled(setOf("OLD"), setOf("NEW"), setOf("OLD", "NEW")))
    }

    @Test
    fun `after rotation an APK signed only with the old key is rejected`() {
        // installed app now has the NEW key; an attacker holding the OLD key offers an old-key-only APK
        assertFalse(UpdateUrlPolicy.lineageAcceptsInstalled(setOf("NEW"), setOf("OLD"), setOf("OLD")))
    }

    @Test
    fun `no readable signer on either side is rejected`() {
        assertFalse(UpdateUrlPolicy.lineageAcceptsInstalled(emptySet<String>(), setOf("A"), setOf("A")))
        assertFalse(UpdateUrlPolicy.lineageAcceptsInstalled(setOf("A"), emptySet(), emptySet()))
    }
}
