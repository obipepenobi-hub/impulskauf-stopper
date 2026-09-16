package com.liam.kaptalismusaufhalter.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure title/price-pairing logic that misfired three times on the same real Amazon
 * cart screenshot: first the old "longest text wins" heuristic picked a recommendations banner,
 * then (after fixing that) "closest title to the first price found" picked the "Einkaufswagen"
 * tab label because the *first* price on screen was the subtotal near the top nav rather than
 * the item's own price further down, and then - after pairing every price with its own nearest
 * title - the exact-match blocklist still let "Alle Einkaufswagen" (the select-all checkbox
 * label) through as a candidate. These run on the plain JVM (no Robolectric) -
 * android.graphics.Rect isn't usable here since its constructor is stubbed out in local unit
 * tests, which is exactly why [PurchaseDetector.pickBestPair] is generic over plain data
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
    fun `rejects the marketing banner that caused the first bug`() {
        val lower = marketingBanner.lowercase()
        assertFalse(PurchaseDetector.isTitleCandidate(marketingBanner, lower))
    }

    @Test
    fun `rejects the Amazon cart tab label that caused the second bug`() {
        val text = "Einkaufswagen"
        assertFalse(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
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
    fun `rejects the select-all checkbox label that caused a third bug`() {
        // "Alle Einkaufswagen" (the "select/deselect all" checkbox above the cart list) isn't
        // in the exact-match blocklist, but slipped through as a "product name" in a real bug
        // report - short chrome labels mentioning the cart word should be rejected generically.
        val text = "Alle Einkaufswagen"
        assertFalse(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
    }

    @Test
    fun `still accepts a real product title even if it were to mention cart words in passing`() {
        // Sanity check that the cart-chrome filter is scoped to short labels only, not any
        // mention of the word anywhere - long real titles should never be affected by it.
        val text = realProductTitle
        assertTrue(PurchaseDetector.isTitleCandidate(text, text.lowercase()))
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

    private data class Item(val text: String, val centerY: Int)
    private data class Price(val value: Double, val centerY: Int)

    @Test
    fun `pairs each price with its own nearest title and picks the closest pair - the exact regression`() {
        // Real layout: a subtotal price sits high up, close to unrelated nav chrome (the
        // "Einkaufswagen" tab) - the actual item's price sits right next to its own title,
        // much further down the screen. Picking "first price found" (the subtotal) and then
        // the nearest title to *that* would wrongly pick the chrome text.
        val chrome = Item("Einkaufswagen", centerY = 300)
        val title = Item(realProductTitle, centerY = 900)

        val subtotal = Price(39.99, centerY = 320) // close to the chrome text
        val itemPrice = Price(39.99, centerY = 905) // right next to the real title

        val (pickedPrice, pickedTitle) = PurchaseDetector.pickBestPair(
            listOf(subtotal, itemPrice),
            listOf(chrome, title),
            priceCenterYOf = { it.centerY },
            titleCenterYOf = { it.centerY },
            titleLengthOf = { it.text.length }
        )

        assertEquals(itemPrice, pickedPrice)
        assertEquals(title, pickedTitle)
    }

    @Test
    fun `falls back to the longest title when no price was found`() {
        val items = listOf(Item("Kurzer Titel", centerY = 50), Item(realProductTitle, centerY = 900))

        val (pickedPrice, pickedTitle) = PurchaseDetector.pickBestPair(
            emptyList<Price>(),
            items,
            priceCenterYOf = { it.centerY },
            titleCenterYOf = { it.centerY },
            titleLengthOf = { it.text.length }
        )

        assertNull(pickedPrice)
        assertEquals(realProductTitle, pickedTitle?.text)
    }

    @Test
    fun `falls back to the first price when there are no title candidates at all`() {
        val prices = listOf(Price(9.99, centerY = 100), Price(39.99, centerY = 900))

        val (pickedPrice, pickedTitle) = PurchaseDetector.pickBestPair(
            prices,
            emptyList<Item>(),
            priceCenterYOf = { it.centerY },
            titleCenterYOf = { it.centerY },
            titleLengthOf = { it.text.length }
        )

        assertEquals(prices.first(), pickedPrice)
        assertNull(pickedTitle)
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
