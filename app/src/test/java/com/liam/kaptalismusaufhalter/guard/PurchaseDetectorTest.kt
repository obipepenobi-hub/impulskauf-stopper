package com.liam.kaptalismusaufhalter.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure title-selection logic that misfired on a real Amazon cart screenshot: the
 * old "longest text wins" heuristic picked a recommendations banner ("Hier sind einige Dinge,
 * be...") instead of the actual product title. These run on the plain JVM (no Robolectric) -
 * android.graphics.Rect isn't usable here since its constructor is stubbed out in local unit
 * tests, which is exactly why [PurchaseDetector.pickBestTitle] is generic over plain data
 * instead of taking Rect directly.
 */
class PurchaseDetectorTest {

    // Approximation of the real Amazon cart-page banner text seen in the bug report.
    private val marketingBanner =
        "Hier sind einige Dinge, bevor du zur Kasse gehst, die dich vielleicht auch interessieren"

    // A realistic long Amazon-style product title (the actual listing from the bug report was
    // truncated on-screen but real titles like this commonly run 100+ characters).
    private val realProductTitle =
        "49CC 2 Takt Pocket Bike Motor Benzinmotor für Fahrrad Motorisiertes Fahrrad 66/80cc Set mit Zubehör"

    @Test
    fun `rejects the marketing banner that caused the original bug`() {
        val lower = marketingBanner.lowercase()
        assertFalse(PurchaseDetector.isTitleCandidate(marketingBanner, lower))
    }

    @Test
    fun `accepts a long realistic product title instead of truncating it away`() {
        val lower = realProductTitle.lowercase()
        assertTrue(PurchaseDetector.isTitleCandidate(realProductTitle, lower))
    }

    @Test
    fun `rejects the checkout button label itself`() {
        val text = "Jetzt kostenpflichtig bestellen"
        assertFalse(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
    }

    @Test
    fun `rejects plain nav chrome like Warenkorb`() {
        val text = "Warenkorb"
        assertFalse(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
    }

    @Test
    fun `rejects text that is just a price`() {
        val text = "39,99 €"
        assertFalse(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
    }

    @Test
    fun `rejects text shorter than the minimum length`() {
        val text = "Deal!"
        assertFalse(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
    }

    private data class Candidate(val text: String, val centerY: Int)

    @Test
    fun `picks the candidate closest to the price over a longer distractor further away`() {
        // Mirrors the real screen layout: the marketing banner sits high up (y=100), the actual
        // product row - title next to its price - sits much further down (y=900).
        val candidates = listOf(
            Candidate(marketingBanner, centerY = 100),
            Candidate(realProductTitle, centerY = 900)
        )
        val priceCenterY = 905 // the price sits almost exactly next to the real title

        val result = PurchaseDetector.pickBestTitle(
            candidates,
            anchorCenterY = priceCenterY,
            centerYOf = { it.centerY },
            lengthOf = { it.text.length }
        )

        assertEquals(realProductTitle, result?.text)
    }

    @Test
    fun `falls back to the longest candidate when there is no price to anchor to`() {
        val candidates = listOf(
            Candidate("Kurzer Titel", centerY = 50),
            Candidate(realProductTitle, centerY = 900)
        )

        val result = PurchaseDetector.pickBestTitle(
            candidates,
            anchorCenterY = null,
            centerYOf = { it.centerY },
            lengthOf = { it.text.length }
        )

        assertEquals(realProductTitle, result?.text)
    }

    @Test
    fun `truncates a very long title for display`() {
        val text = "A".repeat(150)
        val truncated = PurchaseDetector.truncateTitle(text, maxLen = 100)
        assertEquals(101, truncated.length)
        assertTrue(truncated.endsWith("…"))
    }

    @Test
    fun `does not touch a title within the display limit`() {
        assertEquals(realProductTitle, PurchaseDetector.truncateTitle(realProductTitle, maxLen = 150))
    }
}
